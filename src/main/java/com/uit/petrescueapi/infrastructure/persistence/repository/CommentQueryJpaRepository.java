package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.CommentJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.CommentDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.CommentSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentQueryJpaRepository extends JpaRepository<CommentJpaEntity, UUID> {

    @Query("""
        SELECT c.commentId       AS commentId,
               c.postId          AS postId,
               c.parentCommentId AS parentCommentId,
               c.authorId        AS authorId,
               u.username        AS authorUsername,
               c.content         AS content,
               c.likeCount       AS likeCount,
               c.replyCount      AS replyCount,
               c.createdAt       AS createdAt
        FROM CommentJpaEntity c
        LEFT JOIN UserJpaEntity u ON c.authorId = u.userId
        WHERE c.deleted = false
          AND c.postId = :postId
          AND c.parentCommentId IS NULL
        ORDER BY c.createdAt DESC, c.commentId DESC
    """)
    Page<CommentSummaryProjection> findParentCommentsByPostId(
            @Param("postId") UUID postId,
            Pageable pageable
    );

    @Query("""
        SELECT c.commentId       AS commentId,
               c.postId          AS postId,
               c.parentCommentId AS parentCommentId,
               c.authorId        AS authorId,
               u.username        AS authorUsername,
               c.content         AS content,
               c.likeCount       AS likeCount,
               c.replyCount      AS replyCount,
               c.createdAt       AS createdAt
        FROM CommentJpaEntity c
        LEFT JOIN UserJpaEntity u ON c.authorId = u.userId
        WHERE c.deleted = false
          AND c.postId = :postId
          AND c.parentCommentId IS NULL
          AND c.createdAt < :cursor
        ORDER BY c.createdAt DESC, c.commentId DESC
    """)
    Page<CommentSummaryProjection> findParentCommentsByPostIdBeforeCursor(
            @Param("postId") UUID postId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    @Query("""
        SELECT c.commentId       AS commentId,
               c.postId          AS postId,
               c.parentCommentId AS parentCommentId,
               c.authorId        AS authorId,
               u.username        AS authorUsername,
               c.content         AS content,
               c.likeCount       AS likeCount,
               c.replyCount      AS replyCount,
               c.createdAt       AS createdAt
        FROM CommentJpaEntity c
        LEFT JOIN UserJpaEntity u ON c.authorId = u.userId
        WHERE c.deleted = false
          AND c.parentCommentId = :parentCommentId
        ORDER BY c.createdAt ASC, c.commentId ASC
    """)
    Page<CommentSummaryProjection> findRepliesByParentCommentId(
            @Param("parentCommentId") UUID parentCommentId,
            Pageable pageable
    );

    @Query("""
        SELECT c.commentId       AS commentId,
               c.postId          AS postId,
               c.parentCommentId AS parentCommentId,
               c.authorId        AS authorId,
               u.username        AS authorUsername,
               c.content         AS content,
               c.likeCount       AS likeCount,
               c.replyCount      AS replyCount,
               c.createdAt       AS createdAt,
               c.updatedAt       AS updatedAt
        FROM CommentJpaEntity c
        LEFT JOIN UserJpaEntity u ON c.authorId = u.userId
        WHERE c.deleted = false
          AND c.commentId = :commentId
    """)
    Optional<CommentDetailProjection> findDetailById(@Param("commentId") UUID commentId);
}
