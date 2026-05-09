package com.uit.petrescueapi.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCounterServiceTest {

    private StringRedisTemplate redisTemplate;
    private ZSetOperations<String, String> zSetOps;
    private ValueOperations<String, String> valueOps;
    private RedisCounterService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        zSetOps = mock(ZSetOperations.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new RedisCounterService(redisTemplate);
        ReflectionTestUtils.setField(service, "postLikesKeyPattern", "post:%s:likes");
        ReflectionTestUtils.setField(service, "commentLikesKeyPattern", "comment:%s:likes");
        ReflectionTestUtils.setField(service, "postCommentCountKeyPattern", "post:%s:comment_count");
        ReflectionTestUtils.setField(service, "commentReplyCountKeyPattern", "comment:%s:reply_count");
    }

    @Test
    void incrementPostLikesReturnsTrueWhenAdded() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.addIfAbsent(eq(String.format("post:%s:likes", postId)), eq(userId.toString()), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(true);

        boolean added = service.incrementPostLikes(postId, userId);

        assertThat(added).isTrue();
    }

    @Test
    void incrementPostLikesReturnsFalseWhenAlreadyExists() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.addIfAbsent(eq(String.format("post:%s:likes", postId)), eq(userId.toString()), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(false);

        boolean added = service.incrementPostLikes(postId, userId);

        assertThat(added).isFalse();
    }

    @Test
    void checkUserLikedPostUsesZSetScore() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.score(String.format("post:%s:likes", postId), userId.toString())).thenReturn(1.0);

        boolean liked = service.checkUserLikedPost(postId, userId);

        assertThat(liked).isTrue();
    }

    @Test
    void getPostCommentCountReturnsZeroWhenMissing() {
        UUID postId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(null);

        long count = service.getPostCommentCount(postId);

        assertThat(count).isZero();
    }

    @Test
    void getPostCommentCountParsesStoredValue() {
        UUID postId = UUID.randomUUID();
        when(valueOps.get(String.format("post:%s:comment_count", postId))).thenReturn("7");

        long count = service.getPostCommentCount(postId);

        assertThat(count).isEqualTo(7L);
    }

    @Test
    void decrementPostLikesReturnsTrueWhenRemoved() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.remove(String.format("post:%s:likes", postId), userId.toString())).thenReturn(1L);

        boolean removed = service.decrementPostLikes(postId, userId);

        assertThat(removed).isTrue();
    }

    @Test
    void decrementPostLikesReturnsFalseWhenNotPresent() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.remove(String.format("post:%s:likes", postId), userId.toString())).thenReturn(0L);

        boolean removed = service.decrementPostLikes(postId, userId);

        assertThat(removed).isFalse();
    }

    @Test
    void getPostLikeCountReturnsZeroWhenMissing() {
        UUID postId = UUID.randomUUID();
        when(zSetOps.zCard(String.format("post:%s:likes", postId))).thenReturn(null);

        long count = service.getPostLikeCount(postId);

        assertThat(count).isZero();
    }

    @Test
    void getPostLikeCountReturnsStoredValue() {
        UUID postId = UUID.randomUUID();
        when(zSetOps.zCard(String.format("post:%s:likes", postId))).thenReturn(5L);

        long count = service.getPostLikeCount(postId);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void incrementCommentLikesReturnsTrueWhenAdded() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.addIfAbsent(eq(String.format("comment:%s:likes", commentId)), eq(userId.toString()), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(true);

        boolean added = service.incrementCommentLikes(commentId, userId);

        assertThat(added).isTrue();
    }

    @Test
    void incrementCommentLikesReturnsFalseWhenAlreadyExists() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.addIfAbsent(eq(String.format("comment:%s:likes", commentId)), eq(userId.toString()), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(false);

        boolean added = service.incrementCommentLikes(commentId, userId);

        assertThat(added).isFalse();
    }

    @Test
    void decrementCommentLikesReturnsTrueWhenRemoved() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.remove(String.format("comment:%s:likes", commentId), userId.toString())).thenReturn(1L);

        boolean removed = service.decrementCommentLikes(commentId, userId);

        assertThat(removed).isTrue();
    }

    @Test
    void decrementCommentLikesReturnsFalseWhenNotPresent() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.remove(String.format("comment:%s:likes", commentId), userId.toString())).thenReturn(0L);

        boolean removed = service.decrementCommentLikes(commentId, userId);

        assertThat(removed).isFalse();
    }

    @Test
    void getCommentLikeCountReturnsZeroWhenMissing() {
        UUID commentId = UUID.randomUUID();
        when(zSetOps.zCard(String.format("comment:%s:likes", commentId))).thenReturn(null);

        long count = service.getCommentLikeCount(commentId);

        assertThat(count).isZero();
    }

    @Test
    void getCommentLikeCountReturnsStoredValue() {
        UUID commentId = UUID.randomUUID();
        when(zSetOps.zCard(String.format("comment:%s:likes", commentId))).thenReturn(4L);

        long count = service.getCommentLikeCount(commentId);

        assertThat(count).isEqualTo(4L);
    }

    @Test
    void checkUserLikedCommentUsesZSetScore() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(zSetOps.score(String.format("comment:%s:likes", commentId), userId.toString())).thenReturn(1.0);

        boolean liked = service.checkUserLikedComment(commentId, userId);

        assertThat(liked).isTrue();
    }

    @Test
    void incrementPostCommentCountReturnsIncrementedValue() {
        UUID postId = UUID.randomUUID();
        when(valueOps.increment(String.format("post:%s:comment_count", postId))).thenReturn(3L);

        long count = service.incrementPostCommentCount(postId);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void decrementPostCommentCountReturnsZeroAndResetsWhenNegative() {
        UUID postId = UUID.randomUUID();
        when(valueOps.decrement(String.format("post:%s:comment_count", postId))).thenReturn(-1L);

        long count = service.decrementPostCommentCount(postId);

        assertThat(count).isZero();
        verify(valueOps).set(String.format("post:%s:comment_count", postId), "0");
    }

    @Test
    void decrementPostCommentCountReturnsValueWhenNonNegative() {
        UUID postId = UUID.randomUUID();
        when(valueOps.decrement(String.format("post:%s:comment_count", postId))).thenReturn(2L);

        long count = service.decrementPostCommentCount(postId);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void incrementCommentReplyCountReturnsIncrementedValue() {
        UUID parentCommentId = UUID.randomUUID();
        when(valueOps.increment(String.format("comment:%s:reply_count", parentCommentId))).thenReturn(2L);

        long count = service.incrementCommentReplyCount(parentCommentId);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void decrementCommentReplyCountReturnsZeroAndResetsWhenNegative() {
        UUID parentCommentId = UUID.randomUUID();
        when(valueOps.decrement(String.format("comment:%s:reply_count", parentCommentId))).thenReturn(-1L);

        long count = service.decrementCommentReplyCount(parentCommentId);

        assertThat(count).isZero();
        verify(valueOps).set(String.format("comment:%s:reply_count", parentCommentId), "0");
    }

    @Test
    void decrementCommentReplyCountReturnsValueWhenNonNegative() {
        UUID parentCommentId = UUID.randomUUID();
        when(valueOps.decrement(String.format("comment:%s:reply_count", parentCommentId))).thenReturn(1L);

        long count = service.decrementCommentReplyCount(parentCommentId);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void getCommentReplyCountParsesStoredValue() {
        UUID parentCommentId = UUID.randomUUID();
        when(valueOps.get(String.format("comment:%s:reply_count", parentCommentId))).thenReturn("9");

        long count = service.getCommentReplyCount(parentCommentId);

        assertThat(count).isEqualTo(9L);
    }

    @Test
    void getCommentReplyCountReturnsZeroWhenMissing() {
        UUID parentCommentId = UUID.randomUUID();
        when(valueOps.get(String.format("comment:%s:reply_count", parentCommentId))).thenReturn(null);

        long count = service.getCommentReplyCount(parentCommentId);

        assertThat(count).isZero();
    }
}
