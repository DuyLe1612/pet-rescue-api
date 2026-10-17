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
}
