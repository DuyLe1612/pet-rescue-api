package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the RescueCase aggregate.
 */
public interface RescueCaseRepository {

    RescueCase save(RescueCase rescueCase);

    /** Atomic, so two organizations cannot both win. False when the case already has one. */
    boolean assignOrganization(UUID caseId, UUID organizationId);

    Optional<RescueCase> findById(UUID caseId);

    /** Holds the row until commit, so claims on one case run one after another instead of colliding. */
    Optional<RescueCase> findByIdForUpdate(UUID caseId);

    Page<RescueCase> findAll(Pageable pageable);
}
