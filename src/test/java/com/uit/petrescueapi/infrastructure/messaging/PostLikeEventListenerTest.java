package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.PostLikedEvent;
import com.uit.petrescueapi.domain.event.PostUnlikedEvent;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostJpaRepository;
import com.uit.petrescueapi.infrastructure.redis.RedisCounterService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class PostLikeEventListenerTest {

    @Test
    void handlePostLikedUpdatesDbWhenRedisAdded() {
        RedisCounterService redis = mock(RedisCounterService.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, postRepo);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementPostLikes(postId, userId)).thenReturn(true);

        listener.handlePostLiked(PostLikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(postRepo).incrementLikeCount(postId);
    }

    @Test
    void handlePostLikedSkipsDbWhenRedisDuplicate() {
        RedisCounterService redis = mock(RedisCounterService.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, postRepo);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementPostLikes(postId, userId)).thenReturn(false);

        listener.handlePostLiked(PostLikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(postRepo, never()).incrementLikeCount(postId);
    }

    @Test
    void handlePostUnlikedUpdatesDbWhenRedisRemoved() {
        RedisCounterService redis = mock(RedisCounterService.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, postRepo);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.decrementPostLikes(postId, userId)).thenReturn(true);

        listener.handlePostUnliked(PostUnlikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(postRepo).decrementLikeCount(postId);
    }

    @Test
    void handlePostUnlikedSkipsDbWhenRedisHasNoLike() {
        RedisCounterService redis = mock(RedisCounterService.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        PostLikeEventListener listener = new PostLikeEventListener(redis, postRepo);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.decrementPostLikes(postId, userId)).thenReturn(false);

        listener.handlePostUnliked(PostUnlikedEvent.builder()
                .postId(postId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(postRepo, never()).decrementLikeCount(postId);
    }
}
