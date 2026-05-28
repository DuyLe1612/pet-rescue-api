package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.TagJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.TagSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Dedicated read-only JPA repository for Tag queries (CQRS query side).
 *
 * <p>All queries here return <b>interface projections</b> -- Spring Data maps
 * columns to getter methods automatically.</p>
 */
@Repository
public interface TagQueryJpaRepository extends JpaRepository<TagJpaEntity, UUID> {

    // ── Summary (list views) ────────────────────

    @Query("""
        SELECT t.tagId  AS tagId,
               t.code   AS code,
               t.name   AS name
        FROM TagJpaEntity t
        WHERE t.deleted = false
          AND (:search IS NULL OR :search = '' OR
               LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<TagSummaryProjection> findAllSummary(@org.springframework.data.repository.query.Param("search") String search, Pageable pageable);
}
