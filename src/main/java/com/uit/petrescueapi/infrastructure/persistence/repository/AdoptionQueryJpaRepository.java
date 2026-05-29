package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.AdoptionApplicationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.AdoptionDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.AdoptionSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dedicated read-only JPA repository for AdoptionApplication queries (CQRS query side).
 *
 * <p>All queries here use LEFT JOIN to fetch related data in a single SQL statement.
 * Results are returned as <b>interface projections</b> -- Spring Data maps columns
 * to getter methods automatically.</p>
 */
@Repository
public interface AdoptionQueryJpaRepository extends JpaRepository<AdoptionApplicationJpaEntity, UUID> {

    // ── Summary (list views) ────────────────────

    @Query("""
        SELECT a.applicationId   AS applicationId,
               a.adoptionCode    AS adoptionCode,
               p.name            AS petName,
               (SELECT MIN(mf.publicId)
                FROM PetMediaJpaEntity pm
                JOIN MediaFileJpaEntity mf ON pm.mediaFileId = mf.mediaId
                WHERE pm.petId = p.id) AS petPrimaryImageUrl,
               u.username        AS applicantUsername,
               a.status          AS status,
               a.experience      AS experience,
               a.liveCondition   AS liveCondition,
               a.createdAt       AS createdAt
         FROM AdoptionApplicationJpaEntity a
         LEFT JOIN PetJpaEntity p ON a.petId = p.id
         LEFT JOIN UserJpaEntity u ON a.applicantId = u.userId
         WHERE a.deleted = false
            AND a.status IN :statuses
            AND (:search IS NULL OR :search = '' OR
                LOWER(a.adoptionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
     """)
        Page<AdoptionSummaryProjection> findAllSummaries(
            @Param("statuses") List<String> statuses,
            @Param("search") String search,
            Pageable pageable);

    // ── Detail (single adoption application) ────

    @Query("""
        SELECT a.applicationId   AS applicationId,
               a.adoptionCode    AS adoptionCode,
               p.name            AS petName,
               (SELECT MIN(mf.publicId)
                FROM PetMediaJpaEntity pm
                JOIN MediaFileJpaEntity mf ON pm.mediaFileId = mf.mediaId
                WHERE pm.petId = p.id) AS petPrimaryImageUrl,
               u.username        AS applicantUsername,
               a.status          AS status,
               a.experience      AS experience,
               a.liveCondition   AS liveCondition,
               a.createdAt       AS createdAt,
               a.petId           AS petId,
               a.applicantId     AS applicantId,
               a.organizationId  AS organizationId,
               o.name            AS organizationName,
               a.note            AS note,
               a.decidedAt       AS decidedAt,
               a.decidedBy       AS decidedBy,
             d.username        AS decidedByUsername,
             a.rejectReason    AS rejectReason,
             a.readyAt         AS readyAt
        FROM AdoptionApplicationJpaEntity a
        LEFT JOIN PetJpaEntity p ON a.petId = p.id
        LEFT JOIN UserJpaEntity u ON a.applicantId = u.userId
        LEFT JOIN OrganizationJpaEntity o ON a.organizationId = o.organizationId
        LEFT JOIN UserJpaEntity d ON a.decidedBy = d.userId
        WHERE a.deleted = false AND a.applicationId = :id
    """)
    Optional<AdoptionDetailProjection> findDetailById(@Param("id") UUID id);

    @Query("""
        SELECT a.applicationId   AS applicationId,
               a.adoptionCode    AS adoptionCode,
               p.name            AS petName,
               (SELECT MIN(mf.publicId)
                FROM PetMediaJpaEntity pm
                JOIN MediaFileJpaEntity mf ON pm.mediaFileId = mf.mediaId
                WHERE pm.petId = p.id) AS petPrimaryImageUrl,
               u.username        AS applicantUsername,
               a.status          AS status,
               a.experience      AS experience,
               a.liveCondition   AS liveCondition,
               a.createdAt       AS createdAt,
               a.readyAt         AS readyAt
         FROM AdoptionApplicationJpaEntity a
         LEFT JOIN PetJpaEntity p ON a.petId = p.id
         LEFT JOIN UserJpaEntity u ON a.applicantId = u.userId
         WHERE a.deleted = false
          AND a.applicantId = :applicantId
            AND a.status IN :statuses
            AND (:search IS NULL OR :search = '' OR
                LOWER(a.adoptionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
     """)
        Page<AdoptionSummaryProjection> findByApplicantIdSummaries(
            @Param("applicantId") UUID applicantId,
            @Param("statuses") List<String> statuses,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT a.applicationId   AS applicationId,
               a.adoptionCode    AS adoptionCode,
               p.name            AS petName,
               (SELECT MIN(mf.publicId)
                FROM PetMediaJpaEntity pm
                JOIN MediaFileJpaEntity mf ON pm.mediaFileId = mf.mediaId
                WHERE pm.petId = p.id) AS petPrimaryImageUrl,
               u.username        AS applicantUsername,
               a.status          AS status,
               a.experience      AS experience,
               a.liveCondition   AS liveCondition,
               a.createdAt       AS createdAt,
               a.readyAt         AS readyAt
         FROM AdoptionApplicationJpaEntity a
         LEFT JOIN PetJpaEntity p ON a.petId = p.id
         LEFT JOIN UserJpaEntity u ON a.applicantId = u.userId
         WHERE a.deleted = false
          AND a.organizationId = :organizationId
            AND a.status IN :statuses
            AND (:search IS NULL OR :search = '' OR
                LOWER(a.adoptionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
     """)
        Page<AdoptionSummaryProjection> findByOrganizationIdSummaries(
            @Param("organizationId") UUID organizationId,
            @Param("statuses") List<String> statuses,
            @Param("search") String search,
            Pageable pageable);
}
