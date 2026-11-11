package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.CompletionMediaProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueCaseCompletionProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueQueueItemProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueCaseDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueCaseSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueMapMarkerProjection;
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
 * Dedicated read-only JPA repository for RescueCase queries (CQRS query side).
 *
 * <p>All queries here use LEFT JOIN to fetch related data in a single SQL statement.
 * Results are returned as <b>interface projections</b> -- Spring Data maps columns
 * to getter methods automatically.</p>
 *
 * <p><b>To add a new query endpoint:</b>
 * <ol>
 *   <li>Create a new projection interface in {@code infrastructure/persistence/projection/}</li>
 *   <li>Add a {@code @Query} method here returning that projection</li>
 *   <li>Add a mapping method in the corresponding query adapter</li>
 * </ol></p>
 */
@Repository
public interface RescueCaseQueryJpaRepository extends JpaRepository<RescueCaseJpaEntity, UUID> {

    // ── Summary (list views) ────────────────────

    @Query(value = """
      SELECT rc.case_id       AS caseId,
           rc.case_code     AS caseCode,
           rc.species       AS species,
           rc.priority      AS priority,
           rc.status        AS status,
           rc.reported_at   AS reportedAt,
           rc.location_text AS locationText,
           u.username       AS reporterUsername,
           (
             SELECT mf.public_id
             FROM rescue_media rm
             JOIN media_files mf ON mf.media_id = rm.media_id
             WHERE rm.case_id = rc.case_id
             ORDER BY mf.created_at ASC
             LIMIT 1
           ) AS firstImageUrl
      FROM rescue_cases rc
      LEFT JOIN users u ON rc.reported_by = u.user_id
      WHERE rc.is_deleted = false
         AND (:search IS NULL OR :search = '' OR
              LOWER(rc.case_code) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(rc.species) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(rc.location_text) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:allStatuses = true OR rc.status IN (:statuses))
          AND (:allPriorities = true OR rc.priority IN (:priorities))
          AND (CAST(:species AS VARCHAR) IS NULL OR LOWER(rc.species) = LOWER(CAST(:species AS VARCHAR)))
          AND (CAST(:province AS VARCHAR) IS NULL OR rc.province_code = CAST(:province AS VARCHAR)
               OR LOWER(rc.province_name) = LOWER(CAST(:province AS VARCHAR)))
    """,
            countQuery = """
      SELECT COUNT(rc.case_id)
      FROM rescue_cases rc
      LEFT JOIN users u ON rc.reported_by = u.user_id
      WHERE rc.is_deleted = false
         AND (:search IS NULL OR :search = '' OR
              LOWER(rc.case_code) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(rc.species) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(rc.location_text) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:allStatuses = true OR rc.status IN (:statuses))
          AND (:allPriorities = true OR rc.priority IN (:priorities))
          AND (CAST(:species AS VARCHAR) IS NULL OR LOWER(rc.species) = LOWER(CAST(:species AS VARCHAR)))
          AND (CAST(:province AS VARCHAR) IS NULL OR rc.province_code = CAST(:province AS VARCHAR)
               OR LOWER(rc.province_name) = LOWER(CAST(:province AS VARCHAR)))
    """,
            nativeQuery = true)
    Page<RescueCaseSummaryProjection> findAllSummaries(
            @Param("search") String search,
            @Param("allStatuses") boolean allStatuses,
            @Param("statuses") List<String> statuses,
            @Param("allPriorities") boolean allPriorities,
            @Param("priorities") List<String> priorities,
            @Param("species") String species,
            @Param("province") String province,
            Pageable pageable);

    // ── Detail (single rescue case) ─────────────

    /** Everything the detail screen needs, so it never has to reload the case. A null viewer has no claim. */
    @Query(value = """
        SELECT rc.case_id       AS caseId,
               rc.case_code     AS caseCode,
               rc.species       AS species,
               rc.priority      AS priority,
               rc.status        AS status,
               rc.reported_at   AS reportedAt,
               rc.location_text AS locationText,
               u.username       AS reporterUsername,
               rc.reported_by   AS reportedBy,
               rc.organization_id AS organizationId,
               rc.pet_id        AS petId,
               rc.color         AS color,
               rc.description   AS description,
               ST_Y(rc.location) AS locationLat,
               ST_X(rc.location) AS locationLng,
               rc.ward_name     AS wardName,
               rc.province_name AS provinceName,
               rc.contact_phone AS contactPhone,
               rc.needs_help    AS needsHelp,
               (SELECT COUNT(*) FROM rescue_case_claims c
                 WHERE c.case_id = rc.case_id AND c.status = 'ACTIVE') AS activeClaimCount,
               EXISTS (SELECT 1 FROM rescue_case_claims c
                 WHERE c.case_id = rc.case_id AND c.status = 'ACTIVE' AND c.user_id = CAST(:viewerId AS uuid)) AS viewerHasActiveClaim,
               comp.completion_id AS completionId,
               comp.status      AS completionStatus
        FROM rescue_cases rc
        LEFT JOIN users u ON rc.reported_by = u.user_id
        LEFT JOIN rescue_case_completions comp
               ON comp.case_id = rc.case_id AND comp.status <> 'REJECTED' AND comp.is_deleted = false
        WHERE rc.is_deleted = false AND rc.case_id = :id
    """, nativeQuery = true)
    Optional<RescueCaseDetailProjection> findDetailById(@Param("id") UUID id, @Param("viewerId") UUID viewerId);

    @Query(value = "SELECT mf.public_id FROM media_files mf JOIN rescue_media rm ON rm.media_id = mf.media_id WHERE rm.case_id = :caseId ORDER BY mf.created_at", nativeQuery = true)
    java.util.List<String> findMediaPublicIdsByCaseId(@Param("caseId") UUID caseId);

    // ── Nearby (PostGIS spatial query) ──────────

    @Query(value = """
        SELECT rc.case_id AS caseId,
               rc.case_code AS caseCode,
               rc.species AS species,
               rc.priority AS priority,
               rc.status AS status,
               (rc.reported_at AT TIME ZONE 'UTC') AS reportedAt,
               rc.location_text AS locationText,
               u.username AS reporterUsername,
               (
                 SELECT mf.public_id
                 FROM rescue_media rm
                 JOIN media_files mf ON mf.media_id = rm.media_id
                 WHERE rm.case_id = rc.case_id
                 ORDER BY mf.created_at ASC
                 LIMIT 1
               ) AS firstImageUrl,
               ST_Distance(rc.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distance
        FROM rescue_cases rc
        LEFT JOIN users u ON rc.reported_by = u.user_id
        WHERE rc.is_deleted = false
          AND ST_DWithin(rc.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :distanceMeters)
          AND (:allStatuses = true OR rc.status IN (:statuses))
          AND (:allPriorities = true OR rc.priority IN (:priorities))
          AND (CAST(:species AS VARCHAR) IS NULL OR LOWER(rc.species) = LOWER(CAST(:species AS VARCHAR)))
          AND (CAST(:province AS VARCHAR) IS NULL OR rc.province_code = CAST(:province AS VARCHAR)
               OR LOWER(rc.province_name) = LOWER(CAST(:province AS VARCHAR)))
        ORDER BY distance
    """,
            countQuery = """
                        SELECT COUNT(rc.case_id)
                        FROM rescue_cases rc
                        WHERE rc.is_deleted = false
                          AND ST_DWithin(rc.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :distanceMeters)
          AND (:allStatuses = true OR rc.status IN (:statuses))
          AND (:allPriorities = true OR rc.priority IN (:priorities))
          AND (CAST(:species AS VARCHAR) IS NULL OR LOWER(rc.species) = LOWER(CAST(:species AS VARCHAR)))
          AND (CAST(:province AS VARCHAR) IS NULL OR rc.province_code = CAST(:province AS VARCHAR)
               OR LOWER(rc.province_name) = LOWER(CAST(:province AS VARCHAR)))
                    """,
            nativeQuery = true)
    Page<RescueCaseSummaryProjection> findNearby(
            @Param("lng") double lng,
            @Param("lat") double lat,
            @Param("distanceMeters") double distanceMeters,
            @Param("allStatuses") boolean allStatuses,
            @Param("statuses") List<String> statuses,
            @Param("allPriorities") boolean allPriorities,
            @Param("priorities") List<String> priorities,
            @Param("species") String species,
            @Param("province") String province,
            Pageable pageable);

    // ── Within bounding box (PostGIS spatial query) ──

    @Query(value = """
        SELECT rc.case_id AS caseId,
               rc.case_code AS caseCode,
               rc.species AS species,
               rc.priority AS priority,
               rc.status AS status,
               (rc.reported_at AT TIME ZONE 'UTC') AS reportedAt,
               rc.location_text AS locationText,
               u.username AS reporterUsername,
               (
                 SELECT mf.public_id
                 FROM rescue_media rm
                 JOIN media_files mf ON mf.media_id = rm.media_id
                 WHERE rm.case_id = rc.case_id
                 ORDER BY mf.created_at ASC
                 LIMIT 1
               ) AS firstImageUrl
        FROM rescue_cases rc
        LEFT JOIN users u ON rc.reported_by = u.user_id
        WHERE rc.is_deleted = false
          AND ST_Within(rc.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
          AND (:allStatuses = true OR rc.status IN (:statuses))
          AND (:allPriorities = true OR rc.priority IN (:priorities))
          AND (CAST(:species AS VARCHAR) IS NULL OR LOWER(rc.species) = LOWER(CAST(:species AS VARCHAR)))
          AND (CAST(:province AS VARCHAR) IS NULL OR rc.province_code = CAST(:province AS VARCHAR)
               OR LOWER(rc.province_name) = LOWER(CAST(:province AS VARCHAR)))
    """,
            countQuery = """
                        SELECT COUNT(rc.case_id)
                        FROM rescue_cases rc
                        WHERE rc.is_deleted = false
                          AND ST_Within(rc.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
          AND (:allStatuses = true OR rc.status IN (:statuses))
          AND (:allPriorities = true OR rc.priority IN (:priorities))
          AND (CAST(:species AS VARCHAR) IS NULL OR LOWER(rc.species) = LOWER(CAST(:species AS VARCHAR)))
          AND (CAST(:province AS VARCHAR) IS NULL OR rc.province_code = CAST(:province AS VARCHAR)
               OR LOWER(rc.province_name) = LOWER(CAST(:province AS VARCHAR)))
                    """,
            nativeQuery = true)
    Page<RescueCaseSummaryProjection> findWithinBoundingBox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("allStatuses") boolean allStatuses,
            @Param("statuses") List<String> statuses,
            @Param("allPriorities") boolean allPriorities,
            @Param("priorities") List<String> priorities,
            @Param("species") String species,
            @Param("province") String province,
            Pageable pageable);

    // ══════════════════════════════════════════════════════════════════════════
    // MAP MARKER QUERIES - Ultra-optimized for fast map rendering
    // No JOINs, minimal columns, uses spatial index
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Find all markers in bounding box - optimized for map display.
     * Uses GiST spatial index for fast bbox filtering.
     * Returns only essential fields (~100 bytes per marker).
     */
    @Query(value = """
        SELECT rc.case_id AS caseId,
               rc.case_code AS caseCode,
               ST_Y(rc.location) AS latitude,
               ST_X(rc.location) AS longitude,
               rc.priority AS priority,
               rc.status AS status,
               rc.species AS species,
               (rc.reported_at AT TIME ZONE 'UTC') AS reportedAt
        FROM rescue_cases rc
        WHERE rc.is_deleted = false
          AND rc.status IN ('REPORTED', 'IN_PROGRESS')
          AND rc.location IS NOT NULL
          AND ST_Within(rc.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
        ORDER BY rc.reported_at DESC
        LIMIT 500
    """, nativeQuery = true)
    List<RescueMapMarkerProjection> findMarkersInBounds(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat);

    /**
     * Find markers with filters - for filtered map views.
     * All filters are optional (NULL = no filter).
     */
    @Query(value = """
        SELECT rc.case_id AS caseId,
               rc.case_code AS caseCode,
               ST_Y(rc.location) AS latitude,
               ST_X(rc.location) AS longitude,
               rc.priority AS priority,
               rc.status AS status,
               rc.species AS species,
               (rc.reported_at AT TIME ZONE 'UTC') AS reportedAt
        FROM rescue_cases rc
        WHERE rc.is_deleted = false
          AND rc.location IS NOT NULL
          AND ST_Within(rc.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
          AND rc.status IN (:status)
          AND rc.priority IN (:priority)
          AND (:species IS NULL OR rc.species = :species)
        ORDER BY 
          CASE rc.priority
            WHEN 'CRITICAL' THEN 1
            WHEN 'HIGH' THEN 2
            WHEN 'NORMAL' THEN 3
            WHEN 'LOW' THEN 4
            ELSE 5
          END,
          rc.reported_at DESC
        LIMIT 500
    """, nativeQuery = true)
    List<RescueMapMarkerProjection> findMarkersWithFilters(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("status") java.util.List<String> status,
            @Param("priority") java.util.List<String> priority,
            @Param("species") String species);

    @Query(value = """
        SELECT rc.case_id AS caseId,
               rc.case_code AS caseCode,
               ST_Y(rc.location) AS latitude,
               ST_X(rc.location) AS longitude,
               rc.priority AS priority,
               rc.status AS status,
               rc.species AS species,
               (rc.reported_at AT TIME ZONE 'UTC') AS reportedAt
        FROM rescue_cases rc
        WHERE rc.is_deleted = false
          AND rc.location IS NOT NULL
          AND rc.status IN (:status)
          AND rc.priority IN (:priority)
          AND (:species IS NULL OR rc.species = :species)
        ORDER BY rc.reported_at DESC
        LIMIT 500
    """, nativeQuery = true)
    List<RescueMapMarkerProjection> findMapMarkers(
            @Param("status") java.util.List<String> status,
            @Param("priority") java.util.List<String> priority,
            @Param("species") String species);

    @Query(value = """
    SELECT
        rcc.completion_id AS completionId,
        rcc.case_id AS caseId,
        rc.case_code AS caseCode,
        rcc.rescued_at AS rescuedAt,
        rcc.rescue_note AS rescueNote,
        rcc.location_note AS locationNote,
        rcc.verified_by AS verifiedBy,
        u.username AS verifiedByName,
        rcc.verified_at AS verifiedAt,
        rcc.submitted_by AS submittedBy,
        su.username AS submittedByName,
        rcc.outcome AS outcome,
        rcc.status AS status,
        rcc.reject_reason AS rejectReason
    FROM rescue_case_completions rcc
    JOIN rescue_cases rc
        ON rc.case_id = rcc.case_id
    LEFT JOIN users u
        ON u.user_id = rcc.verified_by
    LEFT JOIN users su
        ON su.user_id = rcc.submitted_by
    WHERE rcc.is_deleted = false
      AND (
            (:isResolved = true AND rcc.verified_at IS NOT NULL)
            OR
            (:isResolved = false AND rcc.verified_at IS NULL AND rcc.status <> 'REJECTED')
        )
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM rescue_case_completions rcc
    WHERE rcc.is_deleted = false
      AND (
            (:isResolved = true AND rcc.verified_at IS NOT NULL)
            OR
            (:isResolved = false AND rcc.verified_at IS NULL AND rcc.status <> 'REJECTED')
        )
    """,
            nativeQuery = true)
    Page<RescueCaseCompletionProjection> findAllCompletion(
            @Param("isResolved") boolean isResolved,
            Pageable pageable);

    @Query(value = """
    SELECT
        rcc.completion_id AS completionId,
        rcc.case_id AS caseId,
        rc.case_code AS caseCode,
        rcc.rescued_at AS rescuedAt,
        rcc.rescue_note AS rescueNote,
        rcc.location_note AS locationNote,
        rcc.verified_by AS verifiedBy,
        u.username AS verifiedByName,
        rcc.verified_at AS verifiedAt,
        rcc.submitted_by AS submittedBy,
        su.username AS submittedByName,
        rcc.outcome AS outcome,
        rcc.status AS status,
        rcc.reject_reason AS rejectReason
    FROM rescue_case_completions rcc
    JOIN rescue_cases rc
        ON rc.case_id = rcc.case_id
    LEFT JOIN users u
        ON u.user_id = rcc.verified_by
    LEFT JOIN users su
        ON su.user_id = rcc.submitted_by
    WHERE completion_id = :completionId 
        AND rcc.is_deleted = false
    """,
            nativeQuery = true)
    Optional<RescueCaseCompletionProjection> findCompletionById(
            @Param("completionId") UUID completionId);

    @Query(value = "SELECT mf.public_id FROM media_files mf " +
            "JOIN rescue_completion_media rm ON rm.media_id = mf.media_id " +
            "WHERE rm.completion_id = :completionId " +
            "ORDER BY mf.created_at",
            nativeQuery = true)

    List<String> findMediaPublicIdsByCompletionId(@Param("completionId")UUID completionId);

    /** The images of a whole page in one statement. */
    @Query(value = """
    SELECT rm.completion_id AS completionId, mf.public_id AS publicId
    FROM rescue_completion_media rm
    JOIN media_files mf ON mf.media_id = rm.media_id
    WHERE rm.completion_id IN (:completionIds)
    ORDER BY mf.created_at
    """, nativeQuery = true)
    List<CompletionMediaProjection> findMediaByCompletionIds(@Param("completionIds") java.util.Collection<UUID> completionIds);

    /** Reports only an admin can settle: disputed, overdue, or with no second person to confirm. */
    @Query(value = """
    SELECT
        rcc.completion_id AS completionId,
        rcc.case_id AS caseId,
        rc.case_code AS caseCode,
        rcc.rescued_at AS rescuedAt,
        rcc.rescue_note AS rescueNote,
        rcc.location_note AS locationNote,
        rcc.verified_by AS verifiedBy,
        CAST(NULL AS VARCHAR) AS verifiedByName,
        rcc.verified_at AS verifiedAt,
        rcc.submitted_by AS submittedBy,
        su.username AS submittedByName,
        rcc.outcome AS outcome,
        rcc.status AS status,
        rcc.reject_reason AS rejectReason
    FROM rescue_case_completions rcc
    JOIN rescue_cases rc ON rc.case_id = rcc.case_id
    LEFT JOIN users su ON su.user_id = rcc.submitted_by
    WHERE rcc.is_deleted = false
      AND (rcc.status = 'DISPUTED'
        OR (rcc.status = 'PENDING' AND (rcc.escalated_at IS NOT NULL
            OR rc.reported_by IS NULL
            OR (rc.reported_by = rcc.submitted_by AND rc.organization_id IS NULL))))
    ORDER BY rcc.created_at
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM rescue_case_completions rcc
    JOIN rescue_cases rc ON rc.case_id = rcc.case_id
    WHERE rcc.is_deleted = false
      AND (rcc.status = 'DISPUTED'
        OR (rcc.status = 'PENDING' AND (rcc.escalated_at IS NOT NULL
            OR rc.reported_by IS NULL
            OR (rc.reported_by = rcc.submitted_by AND rc.organization_id IS NULL))))
    """,
            nativeQuery = true)
    Page<RescueCaseCompletionProjection> findCompletionReviewQueue(Pageable pageable);

    /** Unassigned open cases, plus rescued animals waiting for an organization. Null province means everywhere. */
    @Query(value = """
    SELECT
        rc.case_id AS caseId,
        rc.case_code AS caseCode,
        rc.species AS species,
        rc.priority AS priority,
        rc.status AS status,
        rc.description AS description,
        rc.province_name AS provinceName,
        rc.ward_name AS wardName,
        ST_Y(rc.location) AS latitude,
        ST_X(rc.location) AS longitude,
        rc.reported_at AS reportedAt,
        rc.needs_help AS needsHelp,
        (rc.status = 'RESCUED') AS awaitingHandover,
        (SELECT COUNT(*) FROM rescue_case_claims c WHERE c.case_id = rc.case_id AND c.status = 'ACTIVE') AS activeClaimCount
    FROM rescue_cases rc
    WHERE rc.is_deleted = false
      AND rc.organization_id IS NULL
      AND (CAST(:provinceCode AS VARCHAR) IS NULL OR rc.province_code = :provinceCode OR rc.province_code IS NULL)
      AND (rc.status IN ('REPORTED', 'IN_PROGRESS')
        OR (rc.status = 'RESCUED' AND rc.pet_id IS NULL AND EXISTS (
            SELECT 1 FROM rescue_case_completions rcc
            WHERE rcc.case_id = rc.case_id AND rcc.status = 'APPROVED'
              AND rcc.outcome IN ('HANDED_TO_ORG', 'TEMP_HOLDING'))))
    ORDER BY rc.needs_help DESC,
        CASE rc.priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'NORMAL' THEN 2 ELSE 3 END,
        rc.reported_at
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM rescue_cases rc
    WHERE rc.is_deleted = false
      AND rc.organization_id IS NULL
      AND (CAST(:provinceCode AS VARCHAR) IS NULL OR rc.province_code = :provinceCode OR rc.province_code IS NULL)
      AND (rc.status IN ('REPORTED', 'IN_PROGRESS')
        OR (rc.status = 'RESCUED' AND rc.pet_id IS NULL AND EXISTS (
            SELECT 1 FROM rescue_case_completions rcc
            WHERE rcc.case_id = rc.case_id AND rcc.status = 'APPROVED'
              AND rcc.outcome IN ('HANDED_TO_ORG', 'TEMP_HOLDING'))))
    """,
            nativeQuery = true)
    Page<RescueQueueItemProjection> findOrganizationQueue(@Param("provinceCode") String provinceCode, Pageable pageable);
}
