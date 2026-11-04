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
    /** The report that is not rejected; a case has at most one. */
    Optional<RescueCaseCompletion> findOpenByCaseId(UUID caseId);

    java.util.List<RescueCaseCompletion> findPendingToEscalate(java.time.LocalDateTime createdBefore, int limit);
}
