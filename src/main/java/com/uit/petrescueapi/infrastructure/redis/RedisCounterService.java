package com.uit.petrescueapi.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Redis-backed counter and membership service for social features.
 *
 * <p>Likes are stored as ZSETs where member = userId and score = timestamp.
 * Idempotency is guaranteed by ZADD NX semantics (addIfAbsent).</p>
 */
@Service
@RequiredArgsConstructor
public class RedisCounterService {

    private final StringRedisTemplate redis;

    @Value("${app.redis.key.post-likes:post:%s:likes}")
    private String postLikesKeyPattern;

    @Value("${app.redis.key.comment-likes:comment:%s:likes}")
    private String commentLikesKeyPattern;

    @Value("${app.redis.key.post-comment-count:post:%s:comment_count}")
    private String postCommentCountKeyPattern;

    @Value("${app.redis.key.comment-reply-count:comment:%s:reply_count}")
    private String commentReplyCountKeyPattern;

    public boolean incrementPostLikes(UUID postId, UUID userId) {
        String key = postLikesKey(postId);
        String member = userId.toString();
        Boolean added = redis.opsForZSet().addIfAbsent(key, member, System.currentTimeMillis());
        return Boolean.TRUE.equals(added);
    }

    public boolean decrementPostLikes(UUID postId, UUID userId) {
        Long removed = redis.opsForZSet().remove(postLikesKey(postId), userId.toString());
        return removed != null && removed > 0;
    }

    public boolean checkUserLikedPost(UUID postId, UUID userId) {
        return redis.opsForZSet().score(postLikesKey(postId), userId.toString()) != null;
    }

    public boolean incrementCommentLikes(UUID commentId, UUID userId) {
        String key = commentLikesKey(commentId);
        String member = userId.toString();
        Boolean added = redis.opsForZSet().addIfAbsent(key, member, System.currentTimeMillis());
        return Boolean.TRUE.equals(added);
    }

    public boolean decrementCommentLikes(UUID commentId, UUID userId) {
        Long removed = redis.opsForZSet().remove(commentLikesKey(commentId), userId.toString());
        return removed != null && removed > 0;
    }

    public long incrementPostCommentCount(UUID postId) {
        Long value = redis.opsForValue().increment(postCommentCountKey(postId));
        return value == null ? 0L : value;
    }

    public long getPostCommentCount(UUID postId) {
        String value = redis.opsForValue().get(postCommentCountKey(postId));
        return value == null ? 0L : Long.parseLong(value);
    }

    public long incrementCommentReplyCount(UUID parentCommentId) {
        Long value = redis.opsForValue().increment(commentReplyCountKey(parentCommentId));
        return value == null ? 0L : value;
    }

    private String postLikesKey(UUID postId) {
        return String.format(postLikesKeyPattern, postId);
    }

    private String commentLikesKey(UUID commentId) {
        return String.format(commentLikesKeyPattern, commentId);
    }

    private String postCommentCountKey(UUID postId) {
        return String.format(postCommentCountKeyPattern, postId);
    }

    private String commentReplyCountKey(UUID parentCommentId) {
        return String.format(commentReplyCountKeyPattern, parentCommentId);
    }
}
