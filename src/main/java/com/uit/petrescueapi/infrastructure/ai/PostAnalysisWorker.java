package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.domain.event.PostContentSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Runs the AI analysis off the request thread. New/edited posts are analysed right after the
 * write commits; a scheduled pass retries PENDING/FAILED posts (lost events, API outages) up to
 * {@link #MAX_ATTEMPTS} times. Exceptions are logged, never propagated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.storage.post:postgres}' == 'mongo' and '${app.ai.post-analysis.enabled:false}' == 'true'")
public class PostAnalysisWorker {

    static final int MAX_ATTEMPTS = 3;
    private static final int BATCH = 20;

    private final PostAnalysisCommandPort analysis;
    private final PostAnalysisStorePort store;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onPostSubmitted(PostContentSubmittedEvent event) {
        analyseSafely(event.postId());
    }

    @Scheduled(fixedDelayString = "${app.ai.post-analysis.retry-delay-ms:60000}",
            initialDelayString = "${app.ai.post-analysis.retry-delay-ms:60000}")
    public void retryQueuedPosts() {
        // skip posts touched in the last minute: their event-driven attempt may still be running
        for (UUID postId : store.findRetryable(MAX_ATTEMPTS, LocalDateTime.now().minusMinutes(1), BATCH)) {
            analyseSafely(postId);
        }
    }

    private void analyseSafely(UUID postId) {
        try {
            analysis.analyze(postId);
        } catch (RuntimeException e) {
            log.warn("Post analysis for {} failed: {}", postId, e.getMessage(), e);
        }
    }
}
