package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueCaseTriage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescueCaseTriageRepository {

    Optional<RescueCaseTriage> findByCaseId(UUID caseId);

    /**
     * Takes the case for one worker: creates the row if there is none, then moves it to PENDING with
     * one more attempt. False when the case is already analysed, out of attempts, or another worker
     * holds a claim newer than {@code staleBefore}.
     */
    boolean claim(UUID caseId, int maxAttempts, LocalDateTime staleBefore);

    void markAnalyzed(RescueCaseTriage result);

    void markFailed(UUID caseId, String failure, String rawOutput, Long latencyMs);

    /**
     * Open cases that still need a triage: no row yet (a lost event) or a pending or failed one with
     * attempts left, not touched since {@code idleBefore}, reported after {@code reportedAfter}.
     */
    List<UUID> findRetryable(int maxAttempts, LocalDateTime idleBefore, LocalDateTime reportedAfter, int limit);
}
