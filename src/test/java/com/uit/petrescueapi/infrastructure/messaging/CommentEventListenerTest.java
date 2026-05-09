package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.event.CommentLikedEvent;
import com.uit.petrescueapi.domain.event.CommentUnlikedEvent;
import com.uit.petrescueapi.infrastructure.persistence.repository.CommentJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostJpaRepository;
import com.uit.petrescueapi.infrastructure.redis.RedisCounterService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CommentEventListenerTest {

    @Test
    void handleCommentCreatedIncrementsPostAndParentCounters() {
        RedisCounterService redis = mock(RedisCounterService.class);
        CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        CommentEventListener listener = new CommentEventListener(redis, commentRepo, postRepo);

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
        verify(postRepo).incrementCommentCount(postId);
        verify(redis).incrementCommentReplyCount(parentId);
        verify(commentRepo).incrementReplyCount(parentId);
    }

    @Test
    void handleCommentCreatedWithoutParentOnlyIncrementsPostCounter() {
        RedisCounterService redis = mock(RedisCounterService.class);
        CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        CommentEventListener listener = new CommentEventListener(redis, commentRepo, postRepo);

        UUID postId = UUID.randomUUID();

        listener.handleCommentCreated(CommentCreatedEvent.builder()
                .commentId(UUID.randomUUID())
                .postId(postId)
                .parentCommentId(null)
                .authorId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build());

        verify(redis).incrementPostCommentCount(postId);
        verify(postRepo).incrementCommentCount(postId);
        verify(redis, never()).incrementCommentReplyCount(any());
        verify(commentRepo, never()).incrementReplyCount(any());
    }

    @Test
    void handleCommentLikedUpdatesDbWhenRedisAdded() {
        RedisCounterService redis = mock(RedisCounterService.class);
        CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        CommentEventListener listener = new CommentEventListener(redis, commentRepo, postRepo);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementCommentLikes(commentId, userId)).thenReturn(true);

        listener.handleCommentLiked(CommentLikedEvent.builder()
                .commentId(commentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(commentRepo).incrementLikeCount(commentId);
    }

    @Test
    void handleCommentLikedSkipsDbWhenRedisDuplicate() {
        RedisCounterService redis = mock(RedisCounterService.class);
        CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        CommentEventListener listener = new CommentEventListener(redis, commentRepo, postRepo);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.incrementCommentLikes(commentId, userId)).thenReturn(false);

        listener.handleCommentLiked(CommentLikedEvent.builder()
                .commentId(commentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(commentRepo, never()).incrementLikeCount(commentId);
    }

    @Test
    void handleCommentUnlikedUpdatesDbWhenRedisRemoved() {
        RedisCounterService redis = mock(RedisCounterService.class);
        CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        CommentEventListener listener = new CommentEventListener(redis, commentRepo, postRepo);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.decrementCommentLikes(commentId, userId)).thenReturn(true);

        listener.handleCommentUnliked(CommentUnlikedEvent.builder()
                .commentId(commentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(commentRepo).decrementLikeCount(commentId);
    }

    @Test
    void handleCommentUnlikedSkipsDbWhenRedisHasNoLike() {
        RedisCounterService redis = mock(RedisCounterService.class);
        CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
        PostJpaRepository postRepo = mock(PostJpaRepository.class);
        CommentEventListener listener = new CommentEventListener(redis, commentRepo, postRepo);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redis.decrementCommentLikes(commentId, userId)).thenReturn(false);

        listener.handleCommentUnliked(CommentUnlikedEvent.builder()
                .commentId(commentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build());

        verify(commentRepo, never()).decrementLikeCount(commentId);
    }
}
