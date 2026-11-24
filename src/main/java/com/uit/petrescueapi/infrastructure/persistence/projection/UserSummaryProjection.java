package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

/**
 * Spring Data interface projection for user summary list queries.
 *
 * <p>Maps from JPQL {@code SELECT ... FROM users u} query.
 * Column aliases must match getter names (camelCase).</p>
 */
public interface UserSummaryProjection {

    UUID getUserId();
    String getUserCode();
    String getUsername();

    String getFullName();
    String getEmail();
    String getStatus();
}
