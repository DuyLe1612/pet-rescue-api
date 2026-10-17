package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.PetOwnership;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain repository contract for PetOwnership aggregate.
 * Tracks ownership history of pets (user or organization).
 */
public interface PetOwnershipRepository {

    PetOwnership save(PetOwnership ownership);

    /**
     * End current ownership by setting toTime.
     * Used when transferring pet to a new owner.
     */
    void endOwnership(UUID petId, LocalDateTime endTime);
}
