package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

/**
 * Projection for public user search results.
 */
public interface UserPublicSearchProjection {

    UUID getUserId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
}
