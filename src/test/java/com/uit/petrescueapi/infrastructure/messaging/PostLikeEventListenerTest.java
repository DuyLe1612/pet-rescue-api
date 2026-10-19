package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.PostLikedEvent;
import com.uit.petrescueapi.domain.event.PostUnlikedEvent;
import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.infrastructure.redis.RedisCounterService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class PostLikeEventListenerTest {

    @Test
    void handlePostLikedUpdatesDbWhenRedisAdded() {
        RedisCounterService redis = mock(RedisCounterService.class);
        SocialCounterPort counters = mock(SocialCounterPort.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, counters);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementPostLikes(postId, userId)).thenReturn(true);

        listener.handlePostLiked(PostLikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(counters).addPostLikes(postId, 1);
    }

    @Test
    void handlePostLikedSkipsDbWhenRedisDuplicate() {
        RedisCounterService redis = mock(RedisCounterService.class);
        SocialCounterPort counters = mock(SocialCounterPort.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, counters);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementPostLikes(postId, userId)).thenReturn(false);

        listener.handlePostLiked(PostLikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(counters, never()).addPostLikes(any(), anyInt());
    }

    @Test
    void handlePostUnlikedUpdatesDbWhenRedisRemoved() {
        RedisCounterService redis = mock(RedisCounterService.class);
        SocialCounterPort counters = mock(SocialCounterPort.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, counters);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.decrementPostLikes(postId, userId)).thenReturn(true);

        listener.handlePostUnliked(PostUnlikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(counters).addPostLikes(postId, -1);
    }
}
