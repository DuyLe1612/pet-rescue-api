package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the AdoptionApplication aggregate.
 */
public interface AdoptionApplicationRepository {

    AdoptionApplication save(AdoptionApplication application);

    Optional<AdoptionApplication> findById(UUID applicationId);

    long countByApplicantIdAndStatus(UUID applicantId, AdoptionApplicationStatus status);

    /** Applicants whose PENDING or APPROVED application for the pet is about to be closed by {@link #cancelOpenForPet}. */
    java.util.List<UUID> findOpenApplicantIdsForPet(UUID petId, UUID exceptApplicationId);

    /**
     * Open (PENDING or APPROVED) applications with no match score although both sides can now be
     * compared: the applicant has an adopter profile and the pet has traits. Oldest first.
     */
    java.util.List<UUID> findOpenWithoutMatch(int limit);

    /** Cancels every PENDING or APPROVED application for the pet except the given one. */
    int cancelOpenForPet(UUID petId, UUID exceptApplicationId, String reason, java.time.LocalDateTime now);
}
