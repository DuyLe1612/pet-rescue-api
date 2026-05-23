package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

public interface FriendSummaryProjection {
    UUID getUserId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
}
