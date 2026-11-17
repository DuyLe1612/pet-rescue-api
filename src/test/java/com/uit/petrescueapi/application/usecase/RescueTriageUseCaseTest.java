package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisModelDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseTriage;
import com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RescueTriageUseCaseTest {

    final RescueCaseTriageRepository triage = mock(RescueCaseTriageRepository.class);
    final PostAnalysisPort ai = mock(PostAnalysisPort.class);
    final AnalysisCatalogPort catalog = mock(AnalysisCatalogPort.class);
    final PostAnalysisCommandPort models = mock(PostAnalysisCommandPort.class);
    final RescueCaseDomainService cases = mock(RescueCaseDomainService.class);
    final UUID caseId = UUID.randomUUID();

    RescueTriageUseCase useCase = build(Optional.of(ai));

    private RescueTriageUseCase build(Optional<PostAnalysisPort> port) {
        return new RescueTriageUseCase(triage, port, catalog, new PostAnalysisValidator(), models, cases);
    }

    @BeforeEach
    void anOpenCaseAndAnIdleStore() {
        when(cases.findById(caseId)).thenReturn(RescueCase.builder().caseId(caseId).status(RescueCaseStatus.REPORTED)
                .species("Chó").description("Bị xe đụng, chảy máu ở chân sau").locationText("Q7").build());
        when(catalog.speciesNames()).thenReturn(List.of("Cat", "Dog"));
        when(models.modelSettings()).thenReturn(new PostAnalysisModelDto("openrouter/openai/gpt-oss-120b", "claude-opus-5-5", true, List.of(), true));
        when(triage.claim(eq(caseId), anyInt(), any())).thenReturn(true);
        when(triage.countPhotos(caseId)).thenReturn(0);
    }

    private static PostAnalysisSuggestion suggestion(boolean needsRescue, RescuePriority urgency, String species) {
        return new PostAnalysisSuggestion(PostCategory.RESCUE, species, "Q7", "Quận 7", null, needsRescue,
                needsRescue ? "Bleeding from the hind leg" : null, urgency, List.of(), 0.9, 0.9, 0.8);
    }

    private static PostAnalysisPort.Result answer(PostAnalysisSuggestion s) {
        return new PostAnalysisPort.Result("openrouter/openai/gpt-oss-120b", 1, "{\"category\":\"RESCUE\"}", s, null, 2100, 940, 480);
    }

    @Test
    void whenTheAiIsOffNothingHappensAtAll() {
        build(Optional.empty()).triage(caseId);

        verifyNoInteractions(triage, cases, models);
    }

    @Test
    void aFinishedCaseNeedsNoLabel() {
        when(cases.findById(caseId)).thenReturn(RescueCase.builder().caseId(caseId).status(RescueCaseStatus.CLOSED).build());

        useCase.triage(caseId);

        verify(triage, never()).claim(any(), anyInt(), any());
        verifyNoInteractions(ai);
    }

    @Test
    void aCaseAlreadyTakenByAnotherWorkerIsNotSentToTheModelAgain() {
        when(triage.claim(eq(caseId), anyInt(), any())).thenReturn(false);

        useCase.triage(caseId);

        verifyNoInteractions(ai);
    }

    @Test
    void theModelReadsTheReportWithTheActiveModelAndTheSpeciesCatalogOnly() {
        when(ai.analyze(any(), any(), any())).thenReturn(answer(suggestion(true, RescuePriority.CRITICAL, "Dog")));

        useCase.triage(caseId);

        ArgumentCaptor<PostAnalysisPort.Catalog> values = ArgumentCaptor.forClass(PostAnalysisPort.Catalog.class);
        verify(ai).analyze(eq("Loài: Chó\nĐịa điểm: Q7\nMô tả: Bị xe đụng, chảy máu ở chân sau"), values.capture(),
                eq("openrouter/openai/gpt-oss-120b"));
        assertThat(values.getValue().speciesNames()).containsExactly("Cat", "Dog");
        assertThat(values.getValue().tagCodes()).isEmpty();
    }

    @Test
    void aFoundEmergencyIsStoredNextToWhatTheRulesFound() {
        when(ai.analyze(any(), any(), any())).thenReturn(answer(suggestion(true, RescuePriority.CRITICAL, "dog")));

        useCase.triage(caseId);

        ArgumentCaptor<RescueCaseTriage> stored = ArgumentCaptor.forClass(RescueCaseTriage.class);
        verify(triage).markAnalyzed(stored.capture());
        RescueCaseTriage t = stored.getValue();
        assertThat(t.getCaseId()).isEqualTo(caseId);
        assertThat(t.getAiUrgency()).isEqualTo(RescuePriority.CRITICAL);
        assertThat(t.getNeedsRescue()).isTrue();
        assertThat(t.getConditionSummary()).isEqualTo("Bleeding from the hind leg");
        assertThat(t.getSpecies()).as("normalised to the catalog name").isEqualTo("Dog");
        assertThat(t.getDistrict()).isEqualTo("Quận 7");
        assertThat(t.getMissingFields()).as("no photo was attached").containsExactly("PHOTO");
        assertThat(t.getModel()).isEqualTo("openrouter/openai/gpt-oss-120b");
        assertThat(t.getRawOutput()).isEqualTo("{\"category\":\"RESCUE\"}");
        assertThat(t.getLatencyMs()).isEqualTo(2100);
        verify(triage, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    void noEmergencyMeansNoUrgency() {
        when(ai.analyze(any(), any(), any())).thenReturn(answer(suggestion(false, RescuePriority.CRITICAL, "Dog")));

        useCase.triage(caseId);

        ArgumentCaptor<RescueCaseTriage> stored = ArgumentCaptor.forClass(RescueCaseTriage.class);
        verify(triage).markAnalyzed(stored.capture());
        assertThat(stored.getValue().getAiUrgency()).as("urgency only counts when the model sees a rescue").isNull();
        assertThat(stored.getValue().getNeedsRescue()).isFalse();
    }

    @Test
    void aSpeciesOutsideTheCatalogIsDroppedNotInvented() {
        when(ai.analyze(any(), any(), any())).thenReturn(answer(suggestion(true, RescuePriority.HIGH, "Dragon")));

        useCase.triage(caseId);

        ArgumentCaptor<RescueCaseTriage> stored = ArgumentCaptor.forClass(RescueCaseTriage.class);
        verify(triage).markAnalyzed(stored.capture());
        assertThat(stored.getValue().getSpecies()).isNull();
        assertThat(stored.getValue().getAiUrgency()).isEqualTo(RescuePriority.HIGH);
    }

    @Test
    void aModelFailureIsRecordedAndNothingElseChanges() {
        when(ai.analyze(any(), any(), any())).thenReturn(
                PostAnalysisPort.Result.failed("openrouter/openai/gpt-oss-120b", 1, "{\"cat", "output truncated (length)", 900));

        useCase.triage(caseId);

        verify(triage).markFailed(caseId, "output truncated (length)", "{\"cat", 900L);
        verify(triage, never()).markAnalyzed(any());
    }

    @Test
    void anAnswerWithoutACategoryIsRejected() {
        PostAnalysisSuggestion noCategory = new PostAnalysisSuggestion(null, "Dog", null, null, null, true, null,
                RescuePriority.HIGH, List.of(), null, null, null);
        when(ai.analyze(any(), any(), any())).thenReturn(answer(noCategory));

        useCase.triage(caseId);

        verify(triage).markFailed(eq(caseId), eq("invalid model output: category missing"), any(), anyLong());
        verify(triage, never()).markAnalyzed(any());
    }
}
