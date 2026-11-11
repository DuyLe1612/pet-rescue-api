package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.dto.post.CommentPreviewDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.dto.post.PostFeedFilter;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.AuthorDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LikeDocuments.PostLikeDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LikeDocuments.PostLikeKey;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Post read side on MongoDB: every screen is one query; post documents already contain the
 * author, image public ids, tags, counters and preview comments (no N+1 media load, no joins).
 */
@Component
@RequiredArgsConstructor
public class MongoPostQueryAdapter implements PostQueryDataPort {

    /** API sort keys (JPA property names) → document paths. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "postId", "_id",
            "likeCount", "stats.likes",
            "commentCount", "stats.comments",
            "authorUsername", "author.username");

    private final MongoTemplate mongo;
    private final CloudStoragePort cloudStorage;

    @Override
    public PostResponseDto findById(UUID postId) {
        PostDocument d = mongo.findOne(query(where("_id").is(postId).and("deleted").is(false)), PostDocument.class);
        if (d == null) {
            throw new ResourceNotFoundException("Post", "postId", postId);
        }
        AuthorDocument author = d.getAuthor();
        return PostResponseDto.builder()
                .postId(d.getId())
                .authorId(author == null ? null : author.getUserId())
                .authorUsername(author == null ? null : author.getUsername())
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .likeCount(d.getStats() == null ? 0 : d.getStats().getLikes())
                .commentCount(d.getStats() == null ? 0 : d.getStats().getComments())
                .rescueCaseId(d.getRescueCase() == null ? null : d.getRescueCase().getCaseId())
                .content(d.getContent())
                .imageUrls(imageUrls(d))
                .tags(d.getTags())
                .facets(facets(d))
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    @Override
    public Page<PostSummaryResponseDto> findAllSummaries(String search, PostFeedFilter filter, Pageable pageable) {
        List<Criteria> and = new ArrayList<>();
        and.add(where("deleted").is(false));
        and.addAll(facetCriteria(filter));
        if (search != null && !search.isBlank()) {
            // substring + case-insensitive, like the old LOWER(..) LIKE '%..%'; input is matched literally
            Pattern pattern = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
            and.add(new Criteria().orOperator(where("content").regex(pattern), where("author.username").regex(pattern)));
        }
        Criteria criteria = new Criteria().andOperator(and);
        long total = mongo.count(query(criteria), PostDocument.class);
        Query q = query(criteria).with(mapSort(pageable.getSort()))
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize());
        q.fields().exclude("analysis");
        List<PostSummaryResponseDto> items = mongo.find(q, PostDocument.class).stream().map(this::toSummary).toList();
        return new PageImpl<>(items, pageable, total);
    }

    @Override
    public PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, UUID cursorId, int size, UUID viewerId, PostFeedFilter filter) {
        List<Criteria> and = new ArrayList<>();
        and.add(where("deleted").is(false));
        and.addAll(facetCriteria(filter));
        if (cursor != null) {
            and.add(cursorId == null
                    ? where("createdAt").lt(cursor)
                    : new Criteria().orOperator(
                            where("createdAt").lt(cursor),
                            where("createdAt").is(cursor).and("_id").lt(cursorId)));
        }
        Query q = query(new Criteria().andOperator(and))
                .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")))
                .limit(size);
        q.fields().exclude("analysis");
        List<PostSummaryResponseDto> items = mongo.find(q, PostDocument.class).stream().map(this::toSummary).toList();
        markLikedBy(viewerId, items);

        PostSummaryResponseDto last = items.isEmpty() ? null : items.get(items.size() - 1);
        return PostCursorResponseDto.builder()
                .items(items)
                .nextCursor(last == null ? null : last.getCreatedAt())
                .nextCursorId(last == null ? null : last.getPostId())
                .hasMore(items.size() == size)
                .build();
    }

    /** Only confirmed facets are filterable: an unconfirmed AI suggestion never decides what people see. */
    private static List<Criteria> facetCriteria(PostFeedFilter filter) {
        List<Criteria> criteria = new ArrayList<>();
        if (filter == null) {
            return criteria;
        }
        if (filter.category() != null) criteria.add(where("facets.category").is(filter.category()));
        if (filter.species() != null) criteria.add(where("facets.species").is(filter.species()));
        if (filter.district() != null) criteria.add(where("facets.district").is(filter.district()));
        if (filter.province() != null) criteria.add(where("facets.province").is(filter.province()));
        if (filter.needsRescue() != null) {
            // Posts confirmed before the flag existed have no value; they count as "does not need rescue".
            criteria.add(filter.needsRescue()
                    ? where("facets.needsRescue").is(true)
                    : where("facets").ne(null).and("facets.needsRescue").ne(true));
        }
        if (filter.tag() != null) criteria.add(where("tags").is(filter.tag()));
        return criteria;
    }

    private static PostFacets facets(PostDocument d) {
        PostDocument.FacetsDocument f = d.getFacets();
        if (f == null || f.getCategory() == null) {
            return null;
        }
        PostCategory category;
        try {
            category = PostCategory.valueOf(f.getCategory());
        } catch (IllegalArgumentException unknownValue) {
            return null;
        }
        return new PostFacets(category, f.getSpecies(), f.getDistrict(), f.getProvince(), Boolean.TRUE.equals(f.getNeedsRescue()));
    }

    /** One lookup for the whole page. Anonymous viewers get no value at all. */
    private void markLikedBy(UUID viewerId, List<PostSummaryResponseDto> items) {
        if (viewerId == null || items.isEmpty()) {
            return;
        }
        List<PostLikeKey> keys = items.stream().map(item -> new PostLikeKey(item.getPostId(), viewerId)).toList();
        Set<UUID> liked = mongo.find(query(where("_id").in(keys)), PostLikeDocument.class).stream()
                .map(like -> like.getId().getPostId())
                .collect(Collectors.toSet());
        items.forEach(item -> item.setLikedByMe(liked.contains(item.getPostId())));
    }

    private Sort mapSort(Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id"));
        }
        List<Sort.Order> orders = sort.stream()
                .map(o -> new Sort.Order(o.getDirection(), SORT_FIELDS.getOrDefault(o.getProperty(), o.getProperty())))
                .toList();
        return Sort.by(orders);
    }

    private List<String> imageUrls(PostDocument d) {
        return d.getMedia().stream().map(m -> cloudStorage.buildUrl(m.getPublicId())).toList();
    }

    private PostSummaryResponseDto toSummary(PostDocument d) {
        AuthorDocument author = d.getAuthor();
        return PostSummaryResponseDto.builder()
                .postId(d.getId())
                .authorId(author == null ? null : author.getUserId())
                .authorUsername(author == null ? null : author.getUsername())
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .content(d.getContent())
                .tags(d.getTags())
                .facets(facets(d))
                .likeCount(d.getStats() == null ? 0 : d.getStats().getLikes())
                .commentCount(d.getStats() == null ? 0 : d.getStats().getComments())
                .createdAt(d.getCreatedAt())
                .imageUrls(imageUrls(d))
                .previewComments(d.getPreviewComments().stream()
                        .map(c -> CommentPreviewDto.builder()
                                .commentId(c.getCommentId())
                                .authorId(c.getAuthor() == null ? null : c.getAuthor().getUserId())
                                .authorUsername(c.getAuthor() == null ? null : c.getAuthor().getUsername())
                                .authorAvatarUrl(c.getAuthor() == null ? null : c.getAuthor().getAvatarUrl())
                                .content(c.getContent())
                                .createdAt(c.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }
}
