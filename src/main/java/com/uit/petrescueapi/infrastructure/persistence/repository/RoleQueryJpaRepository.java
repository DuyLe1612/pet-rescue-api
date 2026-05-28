package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.RoleJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.RoleDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RoleSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Dedicated read-only JPA repository for Role queries (CQRS query side).
 *
 * <p>All queries here return <b>interface projections</b> -- Spring Data maps
 * columns to getter methods automatically.</p>
 */
@Repository
public interface RoleQueryJpaRepository extends JpaRepository<RoleJpaEntity, Integer> {

    // ── Summary (list views) ────────────────────

    @Query("""
        SELECT r.roleId  AS roleId,
               r.code    AS code,
               r.name    AS name
        FROM RoleJpaEntity r
         WHERE (:search IS NULL OR :search = '' OR
             LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
        Page<RoleSummaryProjection> findAllSummary(@Param("search") String search, Pageable pageable);

    // ── Detail (single role) ────────────────────

    @Query("""
        SELECT r.roleId       AS roleId,
               r.code         AS code,
               r.name         AS name,
               r.description  AS description,
               r.createdAt    AS createdAt
        FROM RoleJpaEntity r
        WHERE r.roleId = :id
    """)
    Optional<RoleDetailProjection> findDetailById(@Param("id") Integer id);
}
