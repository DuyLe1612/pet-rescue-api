package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;

/**
 * Interface projection for pet ownership history display queries.
 */
public interface PetOwnershipHistoryProjection {

    String getOwnerType();

    String getOwnerName();

    LocalDateTime getHistoryTimestamp();
}