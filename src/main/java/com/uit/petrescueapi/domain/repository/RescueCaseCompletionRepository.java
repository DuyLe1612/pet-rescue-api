package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the RescueCaseCompletion aggregate.
 */
public interface RescueCaseCompletionRepository {
    RescueCaseCompletion save(RescueCaseCompletion rescueCaseCompletion);
    Optional<RescueCaseCompletion> findById(UUID id);
    Page<RescueCaseCompletion> findAll(Pageable pageable);
    Optional<RescueCaseCompletion> findByCaseId(UUID caseId);
}
