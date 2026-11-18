package com.uit.petrescueapi.infrastructure.matching;

import com.uit.petrescueapi.domain.event.RescueCaseReportedEvent;
import com.uit.petrescueapi.domain.service.LostAndFoundDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Compares every new case with the other side right after it is stored. It is one spatial query,
 * so it runs before the reply; a failure here never fails the report.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseMatchListener {

    private final LostAndFoundDomainService lostAndFound;

    @TransactionalEventListener(fallbackExecution = true)
    public void onCaseReported(RescueCaseReportedEvent event) {
        try {
            lostAndFound.suggestFor(event.caseId());
        } catch (RuntimeException e) {
            log.warn("Could not look for lost and found matches of case {}: {}", event.caseId(), e.getMessage(), e);
        }
    }
}
