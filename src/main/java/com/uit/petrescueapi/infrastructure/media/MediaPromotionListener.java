package com.uit.petrescueapi.infrastructure.media;

import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.domain.event.MediaPromotionRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Moves uploads after the transaction that used them has committed, so no row lock or database
 * connection waits for cloud storage, and a rolled-back action moves nothing.
 *
 * <p>A failed move is not fatal: the file stays in the temp folder under its current public id,
 * which is still what the database points to.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaPromotionListener {

    private final MediaCommandPort media;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void promote(MediaPromotionRequestedEvent event) {
        try {
            media.confirmUpload(event.mediaId(), event.targetFolder());
        } catch (RuntimeException e) {
            log.warn("Could not move media {} to {}: {}", event.mediaId(), event.targetFolder(), e.getMessage());
        }
    }
}
