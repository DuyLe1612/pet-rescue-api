package com.uit.petrescueapi.infrastructure.matching;

import com.uit.petrescueapi.application.port.command.AdoptionCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scores open applications that have no match yet but can now be compared: the applicant filled in
 * the adopter profile, or staff recorded the pet's traits, after the application was sent. Also scores
 * applications loaded straight into the database (the demo seed). Same evaluation as on submit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.matching.backfill-enabled", havingValue = "true", matchIfMissing = true)
public class MatchBackfillWorker {

    private static final int BATCH = 50;

    private final AdoptionCommandPort adoptions;

    @Scheduled(fixedDelayString = "${app.matching.backfill-ms:300000}", initialDelayString = "${app.matching.backfill-initial-ms:20000}")
    public void run() {
        try {
            int scored = adoptions.backfillMatches(BATCH);
            if (scored > 0) {
                log.info("Scored {} open adoption applications that had no match yet", scored);
            }
        } catch (RuntimeException e) {
            log.warn("Match backfill pass failed: {}", e.getMessage(), e);
        }
    }
}
