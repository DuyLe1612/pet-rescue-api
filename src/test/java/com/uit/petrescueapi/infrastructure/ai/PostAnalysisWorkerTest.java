package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.domain.event.PostContentSubmittedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostAnalysisWorkerTest {

    final PostAnalysisCommandPort analysis = mock(PostAnalysisCommandPort.class);
    final PostAnalysisStorePort store = mock(PostAnalysisStorePort.class);
    final PostAnalysisWorker worker = new PostAnalysisWorker(analysis, store);

    @Test
    void submittedPostIsAnalysed() {
        UUID id = UUID.randomUUID();
        worker.onPostSubmitted(new PostContentSubmittedEvent(id));
        verify(analysis).analyze(id);
    }

    @Test
    void failureNeverEscapesTheListener() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("mongo down")).when(analysis).analyze(id);
        worker.onPostSubmitted(new PostContentSubmittedEvent(id)); // must not throw
    }

    @Test
    void retryPassAnalysesEveryQueuedPostEvenIfOneFails() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(store.findRetryable(eq(PostAnalysisWorker.MAX_ATTEMPTS), anyInt(), any(), any(), anyInt())).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("boom")).when(analysis).analyze(a);

        worker.retryQueuedPosts();

        verify(analysis).analyze(a);
        verify(analysis).analyze(b);
    }
}
