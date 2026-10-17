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

/** One per (user, conversation): everything the conversation-list screen shows (migrate.md §2.2). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(MongoCollections.INBOXES)
public class InboxDocument {
    @Id
    private String id;
    private UUID userId;
    private UUID conversationId;
    private String type;
    private String name;
    private UUID relatedEntityId;
    private String relatedInfo;
    private PeerDocument peer;
    private LastMessageDocument lastMessage;
    private int unread;
    private LocalDateTime sortAt;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private boolean hidden;
    private LocalDateTime createdAt;

    public static String idOf(UUID userId, UUID conversationId) {
        return userId + ":" + conversationId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeerDocument {
        /** Not named {@code id}: Spring Data would map that to {@code _id}. */
        private UUID userId;
        private String name;
        private String avatarUrl;
    }
}
