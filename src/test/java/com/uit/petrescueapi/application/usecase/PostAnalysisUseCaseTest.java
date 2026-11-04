package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.analysis.ConfirmPostAnalysisRequestDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostAnalysisUseCaseTest {

    static final String OPUS = "claude-opus-5-5";
    static final String HAIKU = "claude-haiku-5-5";
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final String CONTENT = "Mn ơi ở Q7 có bé cún bị thương ở chân nằm lâu rồi, ai ở gần có thể giúp bé với ạ.";

    /** In-memory store: just enough state to observe the use case. */
    static class FakeStore implements PostAnalysisStorePort {
        final Map<UUID, AnalysisTarget> posts = new HashMap<>();
        final List<UUID> runs = new ArrayList<>();
        final Map<UUID, List<String>> runErrors = new HashMap<>();
        String failure;
        List<String> confirmedTags;
        int claims;
        String modelOverride;

        @Override public Optional<AnalysisTarget> findTarget(UUID postId) { return Optional.ofNullable(posts.get(postId)); }
        @Override public Optional<Claim> claim(UUID postId, int maxLifetimeAttempts, LocalDateTime staleBefore) {
            AnalysisTarget t = posts.get(postId);
            if (t == null || t.status().isFinal() || claims >= maxLifetimeAttempts) return Optional.empty();
            claims++;
            return Optional.of(new Claim(t, UUID.randomUUID()));
        }
        @Override public UUID saveRun(UUID postId, PostAnalysisPort.Result result, List<String> errors) {
            UUID id = UUID.randomUUID(); runs.add(id); runErrors.put(id, errors); return id;
        }
        @Override public void markAnalyzed(UUID postId, UUID token, UUID runId, String model, PostAnalysisSuggestion s) {
            posts.computeIfPresent(postId, (k, t) -> with(t, PostAnalysisStatus.ANALYZED, s, t.facets(), t.rescueCaseId()));
        }
        @Override public void markFailed(UUID postId, UUID token, UUID runId, String reason) {
            failure = reason;
            posts.computeIfPresent(postId, (k, t) -> with(t, PostAnalysisStatus.FAILED, null, t.facets(), t.rescueCaseId()));
        }
        @Override public void confirm(UUID postId, PostFacets facets, List<String> tags, UUID userId) {
            confirmedTags = tags;
            posts.computeIfPresent(postId, (k, t) -> with(t, PostAnalysisStatus.CONFIRMED, t.suggestion(), facets, t.rescueCaseId()));
        }
        @Override public void reject(UUID postId, UUID userId) {
            posts.computeIfPresent(postId, (k, t) -> with(t, PostAnalysisStatus.REJECTED, t.suggestion(), t.facets(), t.rescueCaseId()));
        }
        @Override public void resetToPending(UUID postId) {
            posts.computeIfPresent(postId, (k, t) -> with(t, PostAnalysisStatus.PENDING, t.suggestion(), t.facets(), t.rescueCaseId()));
        }
        @Override public List<UUID> findRetryable(int maxAttempts, int maxLifetimeAttempts, LocalDateTime before, LocalDateTime staleBefore, int limit) { return List.of(); }
        @Override public List<PostAnalysisMetricsDto> metrics() { return List.of(); }
        @Override public Optional<String> findModelOverride() { return Optional.ofNullable(modelOverride); }
        @Override public void saveModelOverride(String model, UUID changedBy) { modelOverride = model; }
        @Override public void clearModelOverride() { modelOverride = null; }

        static AnalysisTarget with(AnalysisTarget t, PostAnalysisStatus status, PostAnalysisSuggestion s, PostFacets f, UUID caseId) {
            return new AnalysisTarget(t.postId(), t.authorId(), t.content(), status, s, f, caseId, t.model(), t.analyzedAt());
        }
    }

    FakeStore store;
    PostAnalysisPort ai;
    PostAnalysisUseCase useCase;
    UUID postId;

    final PostAnalysisSuggestion good = new PostAnalysisSuggestion(PostCategory.RESCUE, "dog", "Q7", "Quận 7", null,
            true, "Possible leg injury", RescuePriority.HIGH, List.of("injured", "nope"), 0.95, 0.9, 0.8);

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        postId = UUID.randomUUID();
        store.posts.put(postId, new PostAnalysisStorePort.AnalysisTarget(postId, AUTHOR, CONTENT, PostAnalysisStatus.PENDING,
                null, null, null, null, null));
        ai = mock(PostAnalysisPort.class);
        AnalysisCatalogPort catalog = mock(AnalysisCatalogPort.class);
        when(catalog.speciesNames()).thenReturn(List.of("Dog", "Cat"));
        when(catalog.tagCodes()).thenReturn(List.of("injured", "dog"));
        useCase = new PostAnalysisUseCase(store, Optional.of(ai), catalog, new PostAnalysisValidator(),
                OPUS, List.of(HAIKU, OPUS));
    }

    private void aiReturns(PostAnalysisSuggestion s, String failure) {
        when(ai.analyze(eq(CONTENT), any(), any())).thenReturn(new PostAnalysisPort.Result("claude-opus-5-5", 1, "{}", s, failure, 120, 900, 80));
    }

    // ── analyze ─────────────────────────────────────────────

    @Test
    void analyzeStoresValidatedSuggestionAndRun() {
        aiReturns(good, null);

        useCase.analyze(postId);

        var t = store.posts.get(postId);
        assertThat(t.status()).isEqualTo(PostAnalysisStatus.ANALYZED);
        assertThat(t.suggestion().species()).isEqualTo("Dog");
        assertThat(t.suggestion().tags()).containsExactly("injured");
        assertThat(store.runs).hasSize(1);
    }

    @Test
    void modelFailureIsRecordedAsFailedRun() {
        aiReturns(null, "refusal: general_harms");

        useCase.analyze(postId);

        assertThat(store.posts.get(postId).status()).isEqualTo(PostAnalysisStatus.FAILED);
        assertThat(store.failure).contains("refusal");
        assertThat(store.runs).hasSize(1);
    }

    @Test
    void outputWithoutCategoryFailsAndKeepsValidationErrors() {
        aiReturns(new PostAnalysisSuggestion(null, null, null, null, null, false, null, null, List.of(), null, null, null), null);

        useCase.analyze(postId);

        assertThat(store.posts.get(postId).status()).isEqualTo(PostAnalysisStatus.FAILED);
        assertThat(store.runErrors.get(store.runs.get(0))).contains("category missing");
    }

    @Test
    void confirmedPostsAreNeverReanalysed() {
        store.confirm(postId, new PostFacets(PostCategory.GENERAL, null, null, null, false), List.of(), AUTHOR);

        useCase.analyze(postId);

        verifyNoInteractions(ai);
    }

    @Test
    void lifetimeCapStopsPaidCalls() {
        aiReturns(null, "overloaded");
        for (int i = 0; i < PostAnalysisUseCase.MAX_LIFETIME_ATTEMPTS + 2; i++) {
            useCase.analyze(postId);
            store.resetToPending(postId);
        }
        verify(ai, times(PostAnalysisUseCase.MAX_LIFETIME_ATTEMPTS)).analyze(any(), any(), any());
    }

    @Test
    void retryIsOnlyForPendingOrFailedAnalyses() {
        aiReturns(good, null);
        useCase.analyze(postId);                                        // ANALYZED
        assertThatThrownBy(() -> useCase.retry(postId, AUTHOR)).isInstanceOf(BusinessException.class);
    }

    @Test
    void withoutModelAnalyzeIsANoOp() {
        var noAi = new PostAnalysisUseCase(store, Optional.empty(), mock(AnalysisCatalogPort.class), new PostAnalysisValidator(),
                OPUS, List.of(OPUS));
        noAi.analyze(postId);
        assertThat(store.posts.get(postId).status()).isEqualTo(PostAnalysisStatus.PENDING);
    }

    // ── model switch ────────────────────────────────────────

    @Test
    void usesTheConfiguredDefaultModelUntilSwitched() {
        aiReturns(good, null);
        useCase.analyze(postId);
        verify(ai).analyze(eq(CONTENT), any(), eq(OPUS));
        assertThat(useCase.modelSettings().overridden()).isFalse();
    }

    @Test
    void switchAppliesToTheNextCallAndResetGoesBack() {
        aiReturns(good, null);
        var switched = useCase.switchModel(HAIKU, AUTHOR);
        assertThat(switched.activeModel()).isEqualTo(HAIKU);
        assertThat(switched.overridden()).isTrue();

        useCase.analyze(postId);
        verify(ai).analyze(eq(CONTENT), any(), eq(HAIKU));

        assertThat(useCase.resetModel().activeModel()).isEqualTo(OPUS);
    }

    @Test
    void modelOutsideTheAllowedListIsRejected() {
        assertThatThrownBy(() -> useCase.switchModel("claude-fable-5-1", AUTHOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Model not allowed");
        assertThat(store.modelOverride).isNull();
    }

    @Test
    void storedOverrideNoLongerAllowedFallsBackToDefault() {
        store.modelOverride = "claude-removed-from-config";
        assertThat(useCase.modelSettings().activeModel()).isEqualTo(OPUS);
    }

    // ── confirm / reject ────────────────────────────────────

    private ConfirmPostAnalysisRequestDto confirmation(String species, List<String> tags, boolean needsRescue) {
        return new ConfirmPostAnalysisRequestDto(PostCategory.RESCUE, species, "Quận 7", "TP. Hồ Chí Minh", needsRescue, tags);
    }

    @Test
    void authorConfirmsWithEdits() {
        aiReturns(good, null);
        useCase.analyze(postId);

        var dto = useCase.confirm(postId, confirmation("cat", List.of("Dog"), true), AUTHOR);

        assertThat(dto.getStatus()).isEqualTo(PostAnalysisStatus.CONFIRMED);
        assertThat(dto.getFacets()).isEqualTo(new PostFacets(PostCategory.RESCUE, "Cat", "Quận 7", "TP. Hồ Chí Minh", true));
        assertThat(store.confirmedTags).containsExactly("dog");
    }

    @Test
    void onlyTheAuthorMayConfirmOrReject() {
        assertThatThrownBy(() -> useCase.confirm(postId, confirmation("Dog", List.of(), false), OTHER)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> useCase.reject(postId, OTHER)).isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmRejectsValuesOutsideTheCatalog() {
        assertThatThrownBy(() -> useCase.confirm(postId, confirmation("Dragon", List.of(), false), AUTHOR)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> useCase.confirm(postId, confirmation("Dog", List.of("nope"), false), AUTHOR)).isInstanceOf(BusinessException.class);
    }
}
