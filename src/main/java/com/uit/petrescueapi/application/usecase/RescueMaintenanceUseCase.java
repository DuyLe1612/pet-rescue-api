package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.port.command.RescueMaintenancePort;
import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.service.RescueClaimDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

/** No transaction here: each item commits on its own, so one bad row cannot hold back the rest. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RescueMaintenanceUseCase implements RescueMaintenancePort {

    private static final int BATCH = 100;

    private final RescueClaimDomainService claims;
    private final RescueCaseDomainService cases;

    @Value("${app.rescue.claim-reminder-minutes:30}")
    private long reminderMinutes = 30;

    @Value("${app.rescue.completion-review-hours:48}")
    private long reviewHours = 48;

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        claims.findToRemind(now.plusMinutes(reminderMinutes), BATCH).stream().map(RescueCaseClaim::getClaimId)
                .forEach(safely(claims::remind));
        claims.findExpired(now, BATCH).stream().map(RescueCaseClaim::getClaimId)
                .forEach(safely(claims::expire));
        cases.findCompletionsToEscalate(now.minusHours(reviewHours), BATCH).stream().map(RescueCaseCompletion::getCompletionId)
                .forEach(safely(cases::escalateCompletion));
    }

    private static Consumer<UUID> safely(Consumer<UUID> action) {
        return id -> {
            try {
                action.accept(id);
            } catch (RuntimeException e) {
                log.warn("Rescue maintenance failed for {}: {}", id, e.getMessage());
            }
        };
    }
}
