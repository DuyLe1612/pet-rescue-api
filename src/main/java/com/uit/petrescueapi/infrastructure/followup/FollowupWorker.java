package com.uit.petrescueapi.infrastructure.followup;

import com.uit.petrescueapi.application.port.command.FollowupCommandPort;
import com.uit.petrescueapi.domain.service.FollowupDomainService;
import com.uit.petrescueapi.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends due follow-up questions, reminds and closes the ones nobody answered. The schedule is in the
 * database, so an app that slept or restarted sends what it missed on the next pass. The link goes
 * out by email after the milestone is claimed; the same question is also an in-app notification, so a
 * lost email does not lose it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.followup.enabled", havingValue = "true", matchIfMissing = true)
public class FollowupWorker {

    private static final int BATCH = 20;

    private final FollowupCommandPort followups;
    private final EmailService email;

    /** Where the public form lives in the web app; the token is appended. */
    @Value("${app.followup.respond-url:http://localhost:3000/followup}")
    private String respondUrl;

    @Scheduled(fixedDelayString = "${app.followup.poll-ms:60000}", initialDelayString = "${app.followup.poll-ms:60000}")
    public void run() {
        try {
            tick();
        } catch (RuntimeException e) {
            log.warn("Follow-up pass failed: {}", e.getMessage(), e);
        }
    }

    /** One pass; separate from the schedule so it can be called directly. */
    public void tick() {
        for (FollowupDomainService.Delivery delivery : followups.dispatchDue(BATCH)) {
            if (delivery.email() == null) {
                continue;
            }
            try {
                email.sendFollowupEmail(delivery.email(), delivery.adopterName(), delivery.followup().petName(),
                        delivery.followup().milestone(), respondUrl + "?token=" + delivery.token());
            } catch (RuntimeException e) {
                log.warn("Follow-up email for {} was not sent: {}", delivery.followup().followupId(), e.getMessage());
            }
        }
        followups.remind();
        followups.expire();
    }
}
