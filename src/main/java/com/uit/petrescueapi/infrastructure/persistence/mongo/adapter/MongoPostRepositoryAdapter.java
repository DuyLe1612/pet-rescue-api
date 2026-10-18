package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.port.out.PostReferencePort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.repository.PostRepository;
import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;
import com.uit.petrescueapi.infrastructure.config.PostOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.AuthorDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.AnalysisDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.MediaDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.RescueCaseRefDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.StatsDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * {@link PostRepository} on MongoDB. Replaces the PostgreSQL foreign keys: media, tags and the
 * rescue case are validated against PostgreSQL before anything is written, and what the feed
 * renders (media public ids, tag codes, rescue case status, author) is snapshotted into the post.
 */
@Component
@PostOnMongo
@RequiredArgsConstructor
public class MongoPostRepositoryAdapter implements PostRepository {

    private final MongoTemplate mongo;
    private final PostReferencePort references;
    private final UserSnapshotPort userSnapshots;

    @Override
    public Post save(Post post) {
        PostDocument existing = mongo.findById(post.getPostId(), PostDocument.class);

        List<MediaDocument> media = resolveMedia(post.getMediaIds());
        List<String> tags = resolveTags(post.getTagCodes() != null ? post.getTagCodes()
                : existing != null ? existing.getTags() : List.of());
        RescueCaseRefDocument rescueCase = resolveRescueCase(post.getRescueCaseId(), existing);

        Update update = new Update()
                .set("content", post.getContent())
                .set("media", media)
                .set("tags", tags)
                .set("rescueCase", rescueCase)
                .set("updatedAt", post.getUpdatedAt())
                .set("updatedBy", post.getUpdatedBy())
                .set("deleted", post.isDeleted())
                .set("deletedAt", post.getDeletedAt())
                .set("deletedBy", post.getDeletedBy());
        if (existing == null) {
            AuthorDocument author = AuthorDocument.of(post.getAuthorId(),
                    userSnapshots.findByIds(List.of(post.getAuthorId())).get(post.getAuthorId()));
            update.setOnInsert("author", author)
                    .setOnInsert("stats", new StatsDocument())
                    .setOnInsert("previewComments", new ArrayList<>())
                    .setOnInsert("createdAt", post.getCreatedAt())
                    .setOnInsert("createdBy", post.getCreatedBy())
                    // DA2: every new post is queued for AI analysis
                    .setOnInsert("analysis", AnalysisDocument.pending(LocalDateTime.now()))
                    .setOnInsert("rescueSuggestion", "NONE");
        } else if (!Objects.equals(existing.getContent(), post.getContent()) && !isFinal(existing.getAnalysis())) {
            // the suggestion described the old text; a human decision is never discarded
            update.set("analysis.status", PostAnalysisStatus.PENDING.name())
                    .set("analysis.attempts", 0)
                    .set("analysis.lastError", null)
                    .set("analysis.updatedAt", LocalDateTime.now());
        }
        mongo.upsert(query(where("_id").is(post.getPostId())), update, PostDocument.class);

        post.setTagCodes(tags);
        return post;
    }

    @Override
    public Optional<Post> findById(UUID postId) {
        return Optional.ofNullable(mongo.findById(postId, PostDocument.class)).map(MongoPostRepositoryAdapter::toDomain);
    }

    private static boolean isFinal(AnalysisDocument analysis) {
        return analysis != null && analysis.getStatus() != null
                && PostAnalysisStatus.valueOf(analysis.getStatus()).isFinal();
    }

    private List<MediaDocument> resolveMedia(List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }
        List<UUID> distinct = List.copyOf(new LinkedHashSet<>(mediaIds));
        Map<UUID, String> publicIds = references.findMediaPublicIds(distinct);
        List<UUID> missing = distinct.stream().filter(id -> !publicIds.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("Unknown media: " + missing, "POST_MEDIA_NOT_FOUND");
        }
        return distinct.stream().map(id -> new MediaDocument(id, publicIds.get(id))).toList();
    }

    private List<String> resolveTags(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<String> distinct = List.copyOf(new LinkedHashSet<>(codes));
        Set<String> existing = references.findExistingTagCodes(distinct);
        List<String> missing = distinct.stream().filter(code -> !existing.contains(code)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("Unknown tags: " + missing, "POST_TAG_NOT_FOUND");
        }
        return distinct;
    }

    private RescueCaseRefDocument resolveRescueCase(UUID caseId, PostDocument existing) {
        if (caseId == null) {
            return null;
        }
        if (existing != null && existing.getRescueCase() != null && caseId.equals(existing.getRescueCase().getCaseId())) {
            return existing.getRescueCase();
        }
        String status = references.findRescueCaseStatus(caseId)
                .orElseThrow(() -> new BusinessException("Unknown rescue case: " + caseId, "POST_RESCUE_CASE_NOT_FOUND"));
        return new RescueCaseRefDocument(caseId, status);
    }

    static Post toDomain(PostDocument d) {
        return Post.builder()
                .postId(d.getId())
                .authorId(d.getAuthor() == null ? null : d.getAuthor().getUserId())
                .rescueCaseId(d.getRescueCase() == null ? null : d.getRescueCase().getCaseId())
                .content(d.getContent())
                .mediaIds(d.getMedia().stream().map(MediaDocument::getMediaId).toList())
                .tagIds(new ArrayList<>())
                .tagCodes(d.getTags())
                .createdAt(d.getCreatedAt())
                .createdBy(d.getCreatedBy())
                .updatedAt(d.getUpdatedAt())
                .updatedBy(d.getUpdatedBy())
                .deleted(d.isDeleted())
                .deletedAt(d.getDeletedAt())
                .deletedBy(d.getDeletedBy())
                .build();
    }
}
