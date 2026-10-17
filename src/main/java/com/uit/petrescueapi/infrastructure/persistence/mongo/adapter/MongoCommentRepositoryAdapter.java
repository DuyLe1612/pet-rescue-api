package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.CommentRepository;
import com.uit.petrescueapi.infrastructure.config.PostOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.AuthorDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.CommentDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.CommentPreviewDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * {@link CommentRepository} on MongoDB. Besides the comment itself it maintains what the
 * parent post shows: the 2 newest root comments ({@code previewComments}) and — on soft delete —
 * the comment / reply counters (creation counters are incremented by the RabbitMQ listener).
 */
@Component
@PostOnMongo
@RequiredArgsConstructor
public class MongoCommentRepositoryAdapter implements CommentRepository {

    private final MongoTemplate mongo;
    private final UserSnapshotPort userSnapshots;

    @Override
    public Comment save(Comment comment) {
        CommentDocument existing = mongo.findById(comment.getCommentId(), CommentDocument.class);
        if (existing == null) {
            insert(comment);
        } else if (!existing.isDeleted() && comment.isDeleted()) {
            softDelete(existing, comment);
        }
        return comment;
    }

    private void insert(Comment comment) {
        // replaces the FK comments.post_id → posts
        if (!mongo.exists(query(where("_id").is(comment.getPostId()).and("deleted").is(false)), PostDocument.class)) {
            throw new ResourceNotFoundException("Post", "postId", comment.getPostId());
        }
        AuthorDocument author = AuthorDocument.of(comment.getAuthorId(),
                userSnapshots.findByIds(List.of(comment.getAuthorId())).get(comment.getAuthorId()));
        CommentDocument doc = CommentDocument.builder()
                .id(comment.getCommentId())
                .postId(comment.getPostId())
                .parentId(comment.getParentCommentId())
                .author(author)
                .content(comment.getContent())
                .stats(new CommentDocument.StatsDocument(
                        comment.getLikeCount() == null ? 0 : comment.getLikeCount(),
                        comment.getReplyCount() == null ? 0 : comment.getReplyCount()))
                .createdAt(comment.getCreatedAt())
                .createdBy(comment.getCreatedBy())
                .build();
        mongo.insert(doc);

        if (doc.getParentId() == null) {
            mongo.updateFirst(query(where("_id").is(doc.getPostId())),
                    new Update().push("previewComments").atPosition(0).slice(PostDocument.PREVIEW_COMMENTS).each(preview(doc)),
                    PostDocument.class);
        }
    }

    private void softDelete(CommentDocument existing, Comment comment) {
        mongo.updateFirst(query(where("_id").is(existing.getId())),
                new Update().set("deleted", true)
                        .set("deletedAt", comment.getDeletedAt())
                        .set("deletedBy", comment.getDeletedBy()),
                CommentDocument.class);

        mongo.updateFirst(query(where("_id").is(existing.getPostId()).and("stats.comments").gt(0)),
                new Update().inc("stats.comments", -1), PostDocument.class);
        if (existing.getParentId() != null) {
            mongo.updateFirst(query(where("_id").is(existing.getParentId()).and("stats.replies").gt(0)),
                    new Update().inc("stats.replies", -1), CommentDocument.class);
        } else {
            refreshPreview(existing.getPostId());
        }
    }

    private void refreshPreview(UUID postId) {
        List<CommentPreviewDocument> newest = mongo.find(
                        query(where("postId").is(postId).and("parentId").is(null).and("deleted").is(false))
                                .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")))
                                .limit(PostDocument.PREVIEW_COMMENTS),
                        CommentDocument.class)
                .stream().map(MongoCommentRepositoryAdapter::preview).toList();
        mongo.updateFirst(query(where("_id").is(postId)), new Update().set("previewComments", newest), PostDocument.class);
    }

    private static CommentPreviewDocument preview(CommentDocument c) {
        return CommentPreviewDocument.builder()
                .commentId(c.getId())
                .author(c.getAuthor())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }

    @Override
    public Optional<Comment> findById(UUID commentId) {
        return Optional.ofNullable(mongo.findById(commentId, CommentDocument.class)).map(MongoCommentRepositoryAdapter::toDomain);
    }

    static Comment toDomain(CommentDocument d) {
        return Comment.builder()
                .commentId(d.getId())
                .postId(d.getPostId())
                .parentCommentId(d.getParentId())
                .authorId(d.getAuthor() == null ? null : d.getAuthor().getUserId())
                .content(d.getContent())
                .likeCount(d.getStats() == null ? 0 : d.getStats().getLikes())
                .replyCount(d.getStats() == null ? 0 : d.getStats().getReplies())
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
