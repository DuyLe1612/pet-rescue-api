package com.uit.petrescueapi.infrastructure.persistence.mongo.document;

import com.uit.petrescueapi.infrastructure.persistence.mongo.MongoCollections;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Likes keyed by a compound {@code _id} — one user, one like, enforced by the primary key
 * exactly like the PostgreSQL composite PK. Field order inside {@code _id} is fixed.
 */
public final class LikeDocuments {

    private LikeDocuments() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Document(MongoCollections.POST_LIKES)
    public static class PostLikeDocument {
        @Id
        private PostLikeKey id;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostLikeKey {
        private UUID postId;
        private UUID userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Document(MongoCollections.COMMENT_LIKES)
    public static class CommentLikeDocument {
        @Id
        private CommentLikeKey id;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentLikeKey {
        private UUID commentId;
        private UUID userId;
    }
}
