package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.CommentLike;
import com.uit.petrescueapi.domain.entity.PostLike;
import com.uit.petrescueapi.domain.repository.CommentLikeRepository;
import com.uit.petrescueapi.domain.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service for like/unlike business rules with idempotency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(transactionManager = "postTransactionManager")
public class LikeDomainService {

    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;

    public boolean likePost(UUID postId, UUID userId) {
        if (postLikeRepository.exists(postId, userId)) {
            return false;
        }

        PostLike postLike = PostLike.builder()
                .postId(postId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        postLikeRepository.save(postLike);
        log.debug("User {} liked post {}", userId, postId);
        return true;
    }

    public boolean unlikePost(UUID postId, UUID userId) {
        if (!postLikeRepository.exists(postId, userId)) {
            return false;
        }

        postLikeRepository.delete(postId, userId);
        log.debug("User {} unliked post {}", userId, postId);
        return true;
    }

    public boolean likeComment(UUID commentId, UUID userId) {
        if (commentLikeRepository.exists(commentId, userId)) {
            return false;
        }

        CommentLike commentLike = CommentLike.builder()
                .commentId(commentId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        commentLikeRepository.save(commentLike);
        log.debug("User {} liked comment {}", userId, commentId);
        return true;
    }

    public boolean unlikeComment(UUID commentId, UUID userId) {
        if (!commentLikeRepository.exists(commentId, userId)) {
            return false;
        }

        commentLikeRepository.delete(commentId, userId);
        log.debug("User {} unliked comment {}", userId, commentId);
        return true;
    }
}
