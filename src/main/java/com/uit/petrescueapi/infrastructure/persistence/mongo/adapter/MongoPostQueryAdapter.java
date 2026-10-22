package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.dto.post.CommentPreviewDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostFeedFilter;
import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.AuthorDocument;
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
import java.util.UUID;
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
                .rescueCaseId(d.getRescueCase() == null ? null : d.getRescueCase().getCaseId())
                .content(d.getContent())
                .imageUrls(imageUrls(d))
                .tags(d.getTags())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    @Override
    public Page<PostSummaryResponseDto> findAllSummaries(String search, Pageable pageable) {
        Criteria criteria = where("deleted").is(false);
        if (search != null && !search.isBlank()) {
            // substring + case-insensitive, like the old LOWER(..) LIKE '%..%'; input is matched literally
            Pattern pattern = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
            criteria = criteria.orOperator(where("content").regex(pattern), where("author.username").regex(pattern));
        }
        long total = mongo.count(query(criteria), PostDocument.class);
        Query q = query(criteria).with(mapSort(pageable.getSort()))
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize());
        List<PostSummaryResponseDto> items = mongo.find(q, PostDocument.class).stream().map(this::toSummary).toList();
        return new PageImpl<>(items, pageable, total);
    }

    @Override
    public PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, UUID cursorId, int size, UUID viewerId, PostFeedFilter filter) {
        List<Criteria> and = new ArrayList<>();
        and.add(where("deleted").is(false));
        if (filter != null) {
            if (filter.category() != null) and.add(where("facets.category").is(filter.category()));
            if (filter.species() != null) and.add(where("facets.species").is(filter.species()));
            if (filter.district() != null) and.add(where("facets.district").is(filter.district()));
            if (filter.tag() != null) and.add(where("tags").is(filter.tag()));
        }
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
        List<PostSummaryResponseDto> items = mongo.find(q, PostDocument.class).stream().map(this::toSummary).toList();

        PostSummaryResponseDto last = items.isEmpty() ? null : items.get(items.size() - 1);
        return PostCursorResponseDto.builder()
                .items(items)
                .nextCursor(last == null ? null : last.getCreatedAt())
                .nextCursorId(last == null ? null : last.getPostId())
                .hasMore(items.size() == size)
                .build();
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
