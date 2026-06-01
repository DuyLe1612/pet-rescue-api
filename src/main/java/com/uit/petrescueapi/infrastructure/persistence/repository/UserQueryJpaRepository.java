package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.UserJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.UserDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.UserPublicSearchProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.UserSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Dedicated read-only JPA repository for User queries (CQRS query side).
 *
 * <p>All queries here return <b>interface projections</b> -- Spring Data maps
 * columns to getter methods automatically.</p>
 */
@Repository
public interface UserQueryJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    // ── Summary (list views, active users only) ─

    @Query("""
        SELECT u.userId       AS userId,
               u.userCode     AS userCode,
               u.username     AS username,
               u.email        AS email,
               u.status       AS status
        FROM UserJpaEntity u
    """)
    Page<UserSummaryProjection> findAllSummary(Pageable pageable);

        @Query("""
         SELECT u.userId       AS userId,
             u.userCode     AS userCode,
             u.username     AS username,
             u.email        AS email,
             u.status       AS status
         FROM UserJpaEntity u
         WHERE (:searchName IS NULL OR :searchName = '' OR
             LOWER(u.username) LIKE LOWER(CONCAT('%', :searchName, '%')) OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :searchName, '%')) OR
             LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchName, '%')) OR
             LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchName, '%')) OR
             LOWER(u.userCode) LIKE LOWER(CONCAT('%', :searchName, '%')))
        """)
        Page<UserSummaryProjection> findAllSummaryWithSearch(@Param("searchName") String searchName, Pageable pageable);

        @Query("""
                SELECT u.userId   AS userId,
                             u.username AS username,
                             u.fullName AS fullName,
                             u.avatarUrl AS avatarUrl
                FROM UserJpaEntity u
                WHERE u.status = 'ACTIVE'
                    AND (:searchName IS NOT NULL AND :searchName <> '' AND (
                             LOWER(u.username) LIKE LOWER(CONCAT('%', :searchName, '%')) OR
                             LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchName, '%'))
                    ))
        """)
        Page<UserPublicSearchProjection> findPublicSearch(@Param("searchName") String searchName, Pageable pageable);

    // ── Detail (single user) ────────────────────

    @Query("""
        SELECT u.userId        AS userId,
               u.userCode      AS userCode,
               u.username      AS username,
               u.email         AS email,
               u.fullName      AS fullName,
               u.phone         AS phone,
               u.gender        AS gender,
               u.streetAddress AS streetAddress,
               u.wardName      AS wardName,
               u.provinceName  AS provinceName,
               u.status        AS status,
               u.avatarUrl     AS avatarUrl,
               u.emailVerified AS emailVerified,
               u.createdAt     AS createdAt,
               u.updatedAt     AS updatedAt,
               o.organizationId AS organizationId,
               o.name          AS organizationName,
               om.role         AS organizationRole
        FROM UserJpaEntity u
        LEFT JOIN OrganizationMemberJpaEntity om ON om.userId = u.userId AND om.status = 'ACTIVE'
        LEFT JOIN OrganizationJpaEntity o ON o.organizationId = om.organizationId
        WHERE u.userId = :id
    """)
    Optional<UserDetailProjection> findDetailById(@Param("id") UUID id);
}
