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

    /**
     * Atomically takes a PENDING/FAILED post (or one whose ANALYZING claim is older than
     * {@code staleBefore}) for one model call and counts the attempt against the post's lifetime
     * budget. Empty when another worker holds it, a human decided, or the budget is spent.
     */
    Optional<Claim> claim(UUID postId, int maxLifetimeAttempts, LocalDateTime staleBefore);

    /** Records one model call (success or failure) — the Goal 4 dataset. Returns the run id. */
    UUID saveRun(UUID postId, PostAnalysisPort.Result result, List<String> validationErrors);

    /** Applied only while {@code token} still holds the claim (a later edit or decision voids it). */
    void markAnalyzed(UUID postId, UUID token, UUID runId, String model, PostAnalysisSuggestion suggestion);

    void markFailed(UUID postId, UUID token, UUID runId, String reason);

    /** Author accepted/edited the suggestion: facets + tags become searchable, the run gets its human label. */
    void confirm(UUID postId, PostFacets facets, List<String> tags, UUID userId);

    void reject(UUID postId, UUID userId);

    /** Back to PENDING so the worker analyses it again (content changed or manual retry). */
    void resetToPending(UUID postId);

    /** Marks the post as having a rescue case being created; false if one exists or is in progress. */
    boolean reserveRescueCase(UUID postId);

    void releaseRescueCase(UUID postId);

    void linkRescueCase(UUID postId, UUID caseId, String status);

    /**
     * PENDING/FAILED posts last touched before {@code before} with fewer than {@code maxAttempts}
     * attempts for the current text, plus ANALYZING claims older than {@code staleBefore}; posts that
     * spent their lifetime budget are left out so they never occupy the queue.
     */
    List<UUID> findRetryable(int maxAttempts, int maxLifetimeAttempts, LocalDateTime before, LocalDateTime staleBefore, int limit);

    List<PostAnalysisMetricsDto> metrics();

    /** Snapshot of everything the use case needs to decide about one post. */
    record Claim(AnalysisTarget target, UUID token) {
    }

    record AnalysisTarget(UUID postId, UUID authorId, String content, PostAnalysisStatus status,
                          PostAnalysisSuggestion suggestion, PostFacets facets, UUID rescueCaseId,
                          String model, LocalDateTime analyzedAt, String rescueSuggestion) {
    }
}
