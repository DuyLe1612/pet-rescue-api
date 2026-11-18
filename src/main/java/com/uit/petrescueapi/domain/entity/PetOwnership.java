package com.uit.petrescueapi.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing a pet ownership record.
 * Tracks which user or organization owns/owned a pet over time.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetOwnership {

    private UUID petId;
    private String ownerType;   // "USER" or "ORGANIZATION"
    private UUID ownerId;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;

    /**
     * Returns true if this ownership is still active (no end time).
     */
    public boolean isCurrent() {
        return toTime == null;
    }

    private com.uit.petrescueapi.domain.valueobject.AcquisitionType acquisitionType;
    private UUID adoptionApplicationId;
}
