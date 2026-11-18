package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.command.RescueTriageCommandPort;
import com.uit.petrescueapi.application.usecase.RescueTriageUseCase;
import com.uit.petrescueapi.domain.event.RescueCaseReportedEvent;
import com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RescueTriageWorkerTest {

    final RescueTriageCommandPort triage = mock(RescueTriageCommandPort.class);
    final RescueCaseTriageRepository store = mock(RescueCaseTriageRepository.class);
    final RescueTriageWorker worker = new RescueTriageWorker(triage, store);

    @Test
    void aReportedCaseIsTriaged() {
        UUID caseId = UUID.randomUUID();

        worker.onCaseReported(new RescueCaseReportedEvent(caseId));

        verify(triage).triage(caseId);
    }

    @Test
    void aFailureIsLoggedNeverRaisedSoAReportIsNeverAffected() {
        UUID caseId = UUID.randomUUID();
        doThrow(new IllegalStateException("model down")).when(triage).triage(caseId);

        assertThatCode(() -> worker.onCaseReported(new RescueCaseReportedEvent(caseId))).doesNotThrowAnyException();
    }

    @Test
    void theRetryPassTriagesWhatStillNeedsIt_andOneBadCaseDoesNotStopTheRest() {
        UUID bad = UUID.randomUUID();
        UUID good = UUID.randomUUID();
        when(store.findRetryable(eq(RescueTriageUseCase.MAX_ATTEMPTS), any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(bad, good));
        doThrow(new IllegalStateException("boom")).when(triage).triage(bad);

        worker.retryOpenCases();

        verify(triage).triage(bad);
        verify(triage).triage(good);
    }

    @Test
    void theWorkerExistsOnlyWhenTheAiIsEnabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(RescueTriageWorker.class)
                .withBean(RescueTriageCommandPort.class, () -> triage)
                .withBean(RescueCaseTriageRepository.class, () -> store);

        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(RescueTriageWorker.class));
        runner.withPropertyValues("app.ai.post-analysis.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(RescueTriageWorker.class));
    }
}
