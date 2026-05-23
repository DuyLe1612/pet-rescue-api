package com.uit.petrescueapi.domain.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    private UUID conversationId;
    private UUID userId;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private int unreadCount;

    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
