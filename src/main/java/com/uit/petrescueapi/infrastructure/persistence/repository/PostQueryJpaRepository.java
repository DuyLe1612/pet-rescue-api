package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PostJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.PostDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.PostSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Dedicated read-only JPA repository for Post queries (CQRS query side).
 *
 * <p>All queries here use LEFT JOIN to fetch related data in a single SQL statement.
 * Results are returned as <b>interface projections</b> -- Spring Data maps columns
 * to getter methods automatically.</p>
 */
@Repository
public interface PostQueryJpaRepository extends JpaRepository<PostJpaEntity, UUID> {

    // ── Summary (list views) ────────────────────

    @Query("""
        SELECT p.postId     AS postId,
               p.authorId   AS authorId,
               u.username   AS authorUsername,
               p.content    AS content,
               p.likeCount  AS likeCount,
               p.commentCount AS commentCount,
               p.createdAt  AS createdAt
        FROM PostJpaEntity p
        LEFT JOIN UserJpaEntity u ON p.authorId = u.userId
        WHERE p.deleted = false
        ORDER BY p.createdAt DESC
    """)
    Page<PostSummaryProjection> findAllSummaries(Pageable pageable);

    @Query("""
    SELECT p.postId       AS postId,
           p.authorId     AS authorId,
           u.username     AS authorUsername,
           p.content      AS content,
           p.likeCount    AS likeCount,
           p.commentCount AS commentCount,
           p.createdAt    AS createdAt
    FROM PostJpaEntity p
    LEFT JOIN UserJpaEntity u ON p.authorId = u.userId
    WHERE p.deleted = false
    ORDER BY p.createdAt DESC, p.postId DESC
""")
    Page<PostSummaryProjection> findFirstFeedPage(Pageable pageable);

    @Query("""
    SELECT p.postId       AS postId,
           p.authorId     AS authorId,
           u.username     AS authorUsername,
           p.content      AS content,
           p.likeCount    AS likeCount,
           p.commentCount AS commentCount,
           p.createdAt    AS createdAt
    FROM PostJpaEntity p
    LEFT JOIN UserJpaEntity u ON p.authorId = u.userId
    WHERE p.deleted = false
      AND (
            p.createdAt < :cursorCreatedAt
            OR (
                p.createdAt = :cursorCreatedAt
                AND p.postId < :cursorPostId
            )
          )
    ORDER BY p.createdAt DESC, p.postId DESC
""")
    Page<PostSummaryProjection> findFeedByCursor(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorPostId") UUID cursorPostId,
            Pageable pageable
    );

    // ── Detail (single post) ────────────────────

    @Query("""
        SELECT p.postId        AS postId,
               u.username      AS authorUsername,
               p.content       AS content,
               p.createdAt     AS createdAt,
               p.authorId      AS authorId,
               p.rescueCaseId  AS rescueCaseId,
               p.updatedAt     AS updatedAt
        FROM PostJpaEntity p
        LEFT JOIN UserJpaEntity u ON p.authorId = u.userId
        LEFT JOIN RescueCaseJpaEntity rc ON p.rescueCaseId = rc.caseId
        WHERE p.deleted = false AND p.postId = :id
    """)
    Optional<PostDetailProjection> findDetailById(@Param("id") UUID id);
}
