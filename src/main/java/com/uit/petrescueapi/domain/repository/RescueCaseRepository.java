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

    Optional<RescueCase> findById(UUID caseId);

    Page<RescueCase> findAll(Pageable pageable);
}
