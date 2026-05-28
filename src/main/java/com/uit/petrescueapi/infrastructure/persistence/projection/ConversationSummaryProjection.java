package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ConversationSummaryProjection {
    UUID getId();
    String getType();
    String getName();
    UUID getOtherUserId();
    String getOtherUserName();
    String getOtherUserAvatarUrl();
    String getRelatedInfo();
    UUID getRelatedEntityId();
    String getLastMessage();
    LocalDateTime getLastTime();
    Integer getUnread();
}
