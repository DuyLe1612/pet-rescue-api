package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.command.RescueTriageCommandPort;
import com.uit.petrescueapi.application.usecase.RescueTriageUseCase;
import com.uit.petrescueapi.domain.event.RescueCaseReportedEvent;
import com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Runs the triage off the request thread. A new case is labelled right after it is saved; a
 * scheduled pass picks up open cases whose event was lost or whose attempt failed, up to
 * {@link RescueTriageUseCase#MAX_ATTEMPTS} times. Exceptions are logged, never propagated: a
 * report is never affected by the AI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.post-analysis.enabled", havingValue = "true")
public class RescueTriageWorker {

    private static final int BATCH = 20;

    private final RescueTriageCommandPort triage;
    private final RescueCaseTriageRepository store;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onCaseReported(RescueCaseReportedEvent event) {
        triageSafely(event.caseId());
    }

    @Scheduled(fixedDelayString = "${app.ai.post-analysis.retry-delay-ms:60000}",
            initialDelayString = "${app.ai.post-analysis.retry-delay-ms:60000}")
    public void retryOpenCases() {
        // a case reported in the last minute still has its own event-driven attempt on the way
        for (UUID caseId : store.findRetryable(RescueTriageUseCase.MAX_ATTEMPTS, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().minusDays(1), BATCH)) {
            triageSafely(caseId);
        }
    }

    private void triageSafely(UUID caseId) {
        try {
            triage.triage(caseId);
        } catch (RuntimeException e) {
            log.warn("Triage of rescue case {} failed: {}", caseId, e.getMessage(), e);
        }
    }
}
