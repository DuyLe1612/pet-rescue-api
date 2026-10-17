package com.uit.petrescueapi.infrastructure.persistence.mongo.document;

import com.uit.petrescueapi.infrastructure.persistence.mongo.MongoCollections;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/** Comment or reply (max two levels, enforced in CommentDomainService), author embedded. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(MongoCollections.COMMENTS)
public class CommentDocument {
    @Id
    private UUID id;
    private UUID postId;
    /** {@code null} = root comment. */
    private UUID parentId;
    private AuthorDocument author;
    private String content;
    @Builder.Default
    private StatsDocument stats = new StatsDocument();
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private UUID deletedBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsDocument {
        private int likes;
        private int replies;
    }
}
