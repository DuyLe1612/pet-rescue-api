package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.event.CommentLikedEvent;
import com.uit.petrescueapi.domain.event.CommentUnlikedEvent;
import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.infrastructure.redis.RedisCounterService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CommentEventListenerTest {

    @Test
    void handleCommentCreatedIncrementsPostAndParentCounters() {
        RedisCounterService redis = mock(RedisCounterService.class);
        SocialCounterPort counters = mock(SocialCounterPort.class);
        CommentEventListener listener = new CommentEventListener(redis, counters);

        UUID postId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        listener.handleCommentCreated(CommentCreatedEvent.builder()
                .commentId(UUID.randomUUID())
                .postId(postId)
                .parentCommentId(parentId)
                .authorId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build());

        verify(redis).incrementPostCommentCount(postId);
        verify(counters).addPostComments(postId, 1);
        verify(redis).incrementCommentReplyCount(parentId);
        verify(counters).addCommentReplies(parentId, 1);
    }

    @Test
    void handleCommentLikedUpdatesDbWhenRedisAdded() {
        RedisCounterService redis = mock(RedisCounterService.class);
        SocialCounterPort counters = mock(SocialCounterPort.class);
        CommentEventListener listener = new CommentEventListener(redis, counters);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementCommentLikes(commentId, userId)).thenReturn(true);

        listener.handleCommentLiked(CommentLikedEvent.builder()
                .commentId(commentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(counters).addCommentLikes(commentId, 1);
    }

    @Test
    void handleCommentUnlikedUpdatesDbWhenRedisRemoved() {
        RedisCounterService redis = mock(RedisCounterService.class);
        SocialCounterPort counters = mock(SocialCounterPort.class);
        CommentEventListener listener = new CommentEventListener(redis, counters);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.decrementCommentLikes(commentId, userId)).thenReturn(true);

        listener.handleCommentUnliked(CommentUnlikedEvent.builder()
                .commentId(commentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(counters).addCommentLikes(commentId, -1);
    }
}
