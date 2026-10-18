package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence of post analysis state and the evaluation dataset (MongoDB only). */
public interface PostAnalysisStorePort {

    Optional<AnalysisTarget> findTarget(UUID postId);

    /** Records one model call (success or failure) — the Goal 4 dataset. Returns the run id. */
    UUID saveRun(UUID postId, PostAnalysisPort.Result result, List<String> validationErrors);

    void markAnalyzed(UUID postId, UUID runId, String model, PostAnalysisSuggestion suggestion);

    void markFailed(UUID postId, UUID runId, String reason);

    /** Author accepted/edited the suggestion: facets + tags become searchable, the run gets its human label. */
    void confirm(UUID postId, PostFacets facets, List<String> tags, UUID userId);

    void reject(UUID postId, UUID userId);

    /** Back to PENDING so the worker analyses it again (content changed or manual retry). */
    void resetToPending(UUID postId);

    void linkRescueCase(UUID postId, UUID caseId, String status);

    /** PENDING/FAILED posts last touched before {@code before} with fewer than {@code maxAttempts} attempts. */
    List<UUID> findRetryable(int maxAttempts, LocalDateTime before, int limit);

    List<PostAnalysisMetricsDto> metrics();

    /** Snapshot of everything the use case needs to decide about one post. */
    record AnalysisTarget(UUID postId, UUID authorId, String content, PostAnalysisStatus status,
                          PostAnalysisSuggestion suggestion, PostFacets facets, UUID rescueCaseId,
                          String model, LocalDateTime analyzedAt, String rescueSuggestion) {
    }
}
