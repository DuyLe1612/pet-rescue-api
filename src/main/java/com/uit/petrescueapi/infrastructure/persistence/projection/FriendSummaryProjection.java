package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;
import java.time.LocalDateTime;

public interface FriendSummaryProjection {
    UUID getUserId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
    LocalDateTime getCreatedAt();
}
