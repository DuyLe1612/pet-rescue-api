package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service for comment business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(transactionManager = "mongoTransactionManager")
public class CommentDomainService {

    private final CommentRepository commentRepository;

    @Transactional(transactionManager = "mongoTransactionManager", readOnly = true)
    public Comment findById(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "commentId", commentId));
    }

    public Comment createComment(Comment comment) {
        if (comment.getPostId() == null) {
            throw new BusinessException("Comment postId is required");
        }
        if (comment.getAuthorId() == null) {
            throw new BusinessException("Comment authorId is required");
        }
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new BusinessException("Comment content must not be blank");
        }

        validateNestingLevel(comment.getParentCommentId());

        comment.setCommentId(UUID.randomUUID());
        comment.setCreatedAt(LocalDateTime.now());
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        if (comment.getReplyCount() == null) {
            comment.setReplyCount(0);
        }

        Comment saved = commentRepository.save(comment);
        log.info("Created comment {} on post {}", saved.getCommentId(), saved.getPostId());
        return saved;
    }

    public void deleteComment(UUID commentId) {
        Comment comment = findById(commentId);
        comment.setDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
        log.info("Soft-deleted comment {}", commentId);
    }

    private void validateNestingLevel(UUID parentCommentId) {
        if (parentCommentId == null) {
            return;
        }

        Comment parent = findById(parentCommentId);
        if (parent.getParentCommentId() != null) {
            throw new BusinessException("Comments support only 2 levels (parent and one reply level)");
        }
    }
}
