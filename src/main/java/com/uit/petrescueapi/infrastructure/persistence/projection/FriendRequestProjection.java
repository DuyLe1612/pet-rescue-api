package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FriendRequestProjection {
    UUID getRequestId();
    UUID getRequesterId();
    UUID getAddresseeId();
    String getRequesterName();
    String getRequesterAvatarUrl();
    String getStatus();
    LocalDateTime getCreatedAt();
}
