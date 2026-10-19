package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.infrastructure.config.PostOnPostgres;
import com.uit.petrescueapi.infrastructure.persistence.repository.CommentJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** PostgreSQL counters — same UPDATE statements the listeners used to call directly. */
@Component
@PostOnPostgres
@RequiredArgsConstructor
public class SocialCounterJpaAdapter implements SocialCounterPort {

    private final PostJpaRepository postJpaRepository;
    private final CommentJpaRepository commentJpaRepository;

    @Override
    @Transactional
    public void addPostLikes(UUID postId, int delta) {
        for (int i = 0; i < Math.abs(delta); i++) {
            if (delta > 0) postJpaRepository.incrementLikeCount(postId);
            else postJpaRepository.decrementLikeCount(postId);
        }
    }

    /** Decrements are not supported on PostgreSQL (soft-deleted comments were never subtracted there). */
    @Override
    @Transactional
    public void addPostComments(UUID postId, int delta) {
        for (int i = 0; i < delta; i++) {
            postJpaRepository.incrementCommentCount(postId);
        }
    }

    @Override
    @Transactional
    public void addCommentLikes(UUID commentId, int delta) {
        for (int i = 0; i < Math.abs(delta); i++) {
            if (delta > 0) commentJpaRepository.incrementLikeCount(commentId);
            else commentJpaRepository.decrementLikeCount(commentId);
        }
    }

    @Override
    @Transactional
    public void addCommentReplies(UUID commentId, int delta) {
        for (int i = 0; i < delta; i++) {
            commentJpaRepository.incrementReplyCount(commentId);
        }
    }
}
