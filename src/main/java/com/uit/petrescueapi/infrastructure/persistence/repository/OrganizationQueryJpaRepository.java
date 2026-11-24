package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationOverviewProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationMapMarkerProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationQueryJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

        @Query("""
             SELECT o.organizationId AS organizationId,
                     o.organizationCode AS organizationCode,
                     o.name AS name,
                     o.type AS type,
                     o.status AS status,
                     o.streetAddress AS streetAddress,
                     o.wardName AS wardName,
                     o.provinceName AS provinceName,
                     o.phone AS phone,
                     o.email AS email,
                     o.imageUrl AS imageUrl
             FROM OrganizationJpaEntity o
             WHERE o.status IN :statuses
                AND (:search IS NULL OR :search = '' OR
                     LOWER(o.organizationCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(o.type) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(o.provinceName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(o.wardName) LIKE LOWER(CONCAT('%', :search, '%')))
             """)
    Page<OrganizationSummaryProjection> findAllSummary(@Param("statuses") List<OrganizationStatus> statuses, @Param("search") String search, Pageable pageable);

    @Query(value = """
            SELECT o.organization_id      AS organizationId,
                   o.organization_code    AS organizationCode,
                   o.name                 AS name,
                   o.description          AS description,
                   o.type                 AS type,
                   o.street_address       AS streetAddress,
                   o.ward_code            AS wardCode,
                   o.ward_name            AS wardName,
                   o.province_code        AS provinceCode,
                   o.province_name        AS provinceName,
                   o.phone                AS phone,
                   o.email                AS email,
                   o.image_url            AS imageUrl,
                   o.official_link        AS officialLink,
                   ST_Y(o.location)       AS latitude,
                   ST_X(o.location)       AS longitude,
                   o.status               AS status,
                   o.requested_by_user_id AS requestedByUserId,
                   req.username           AS requestedByUsername,
                   o.created_by           AS createdBy,
                   (o.created_at AT TIME ZONE 'UTC') AS createdAt,
                   (o.updated_at AT TIME ZONE 'UTC') AS updatedAt
            FROM organizations o
            LEFT JOIN users req ON req.user_id = o.requested_by_user_id
            WHERE o.organization_id = :id
            """, nativeQuery = true)
    Optional<OrganizationDetailProjection> findDetailById(@Param("id") UUID id);

    /** Every count of one centre in one round trip. Pets count by current owner, like the pet list. */
    @Query(value = """
            WITH own AS (
                SELECT p.status, p.health_status
                FROM pets p
                JOIN pets_current_owner co ON co.pet_id = p.pet_id
                WHERE co.owner_type = 'ORGANIZATION' AND co.owner_id = :id AND p.is_deleted = false
            )
            SELECT o.organization_id   AS organizationId,
                   o.organization_code AS organizationCode,
                   NULLIF(BTRIM(req.full_name), '') AS requestedByName,
                   (o.created_at AT TIME ZONE 'UTC') AS createdAt,
                   (SELECT COUNT(*) FROM own WHERE status = 'AVAILABLE') AS availablePets,
                   (SELECT COUNT(*) FROM own WHERE status NOT IN ('ADOPTED', 'DECEASED')) AS petsInCare,
                   (SELECT COUNT(*) FROM own WHERE health_status = 'UNDER_TREATMENT'
                                              AND status NOT IN ('ADOPTED', 'DECEASED')) AS petsUnderTreatment,
                   (SELECT COUNT(*) FROM adoption_applications a
                    WHERE a.organization_id = o.organization_id AND a.is_deleted = false AND a.status = 'COMPLETED') AS adoptedPets,
                   (SELECT COUNT(*) FROM adoption_applications a
                    WHERE a.organization_id = o.organization_id AND a.is_deleted = false AND a.status = 'PENDING') AS pendingAdoptions,
                   (SELECT COUNT(*) FROM adoption_applications a
                    WHERE a.organization_id = o.organization_id AND a.is_deleted = false AND a.status = 'APPROVED') AS awaitingPickup,
                   (SELECT COUNT(*) FROM rescue_case_claims c
                    WHERE c.organization_id = o.organization_id AND c.status = 'ACTIVE') AS activeRescueClaims,
                   (SELECT COUNT(DISTINCT c.case_id) FROM rescue_case_claims c
                    WHERE c.organization_id = o.organization_id AND c.status = 'COMPLETED') AS rescuedCases,
                   (SELECT COUNT(*) FROM followup_alerts f
                    WHERE f.organization_id = o.organization_id AND f.status <> 'RESOLVED') AS openFollowupAlerts,
                   (SELECT COUNT(*) FROM organization_members m
                    WHERE m.organization_id = o.organization_id AND m.status = 'ACTIVE') AS activeMembers
            FROM organizations o
            LEFT JOIN users req ON req.user_id = o.requested_by_user_id
            WHERE o.organization_id = :id
            """, nativeQuery = true)
    Optional<OrganizationOverviewProjection> findOverview(@Param("id") UUID id);

                @Query(value = """
                                                SELECT o.organization_id AS organizationId,
                                                                         o.organization_code AS organizationCode,
                                                                         o.name AS name,
                                                                         o.type AS type,
                                                                         o.status AS status,
                                                                         ST_Y(o.location) AS latitude,
                                                                         ST_X(o.location) AS longitude,
                                                                         o.phone AS phone,
                                                                         o.image_url AS imageUrl,
                                                                         o.street_address AS streetAddress,
                                                                         o.ward_name AS wardName,
                                                                         o.province_name AS provinceName
                                                FROM organizations o
                                                WHERE o.is_deleted = false
                                                        AND o.location IS NOT NULL
                                                        AND o.status IN (:statuses)
                                                        AND o.type IN (:types)
                                                        AND (CAST(:province AS VARCHAR) IS NULL OR o.province_code = CAST(:province AS VARCHAR)
                                                             OR LOWER(o.province_name) = LOWER(CAST(:province AS VARCHAR)))
                                                ORDER BY o.created_at DESC
                                                LIMIT 500
                                                """, nativeQuery = true)
                List<OrganizationMapMarkerProjection> findMapMarkers(@Param("statuses") List<String> statuses,
                                                                                                                                                                                                                                 @Param("types") List<String> types,
                        @Param("province") String province);

                @Query(value = """
                                                SELECT o.organization_id AS organizationId,
                                                                         o.organization_code AS organizationCode,
                                                                         o.name AS name,
                                                                         o.type AS type,
                                                                         o.status AS status,
                                                                         o.street_address AS streetAddress,
                                                                         o.ward_name AS wardName,
                                                                         o.province_name AS provinceName,
                                                                         o.phone AS phone,
                                                                         o.email AS email,
                                                                         o.image_url AS imageUrl
                                                FROM organizations o
                                                WHERE o.is_deleted = false
                                                        AND o.location IS NOT NULL
                                                        AND ST_Within(o.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
                                                        AND o.status IN (:statuses)
                                                        AND (:search IS NULL OR :search = '' OR
                                                             LOWER(o.organization_code) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.type) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.province_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.ward_name) LIKE LOWER(CONCAT('%', :search, '%')))
                                                """,
                                                countQuery = "SELECT COUNT(1) FROM organizations o WHERE o.is_deleted = false AND o.location IS NOT NULL AND ST_Within(o.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)) AND o.status IN (:statuses) AND (:search IS NULL OR :search = '' OR LOWER(o.organization_code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.type) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.province_name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.ward_name) LIKE LOWER(CONCAT('%', :search, '%'))) ",
                                                nativeQuery = true)
                Page<OrganizationSummaryProjection> findWithinBoundingBoxSummary(@Param("minLat") double minLat,
                                                                                                                                                                                                                                                                                        @Param("minLng") double minLng,
                                                                                                                                                                                                                                                                                        @Param("maxLat") double maxLat,
                                                                                                                                                                                                                                                                                        @Param("maxLng") double maxLng,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        @Param("statuses") List<String> statuses,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        @Param("search") String search,
                                                                                                                                                                                                                                                                                        Pageable pageable);

                @Query(value = """
                                                SELECT o.organization_id AS organizationId,
                                                                         o.organization_code AS organizationCode,
                                                                         o.name AS name,
                                                                         o.type AS type,
                                                                         o.status AS status,
                                                                         ST_Y(o.location) AS latitude,
                                                                         ST_X(o.location) AS longitude,
                                                                         o.phone AS phone,
                                                                         o.image_url AS imageUrl,
                                                                         o.street_address AS streetAddress,
                                                                         o.ward_name AS wardName,
                                                                         o.province_name AS provinceName
                                                FROM organizations o
                                                WHERE o.is_deleted = false
                                                        AND o.location IS NOT NULL
                                                        AND ST_Within(o.location, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
                                                        AND o.status IN (:statuses)
                                                        AND o.type IN (:types)
                                                        AND (CAST(:province AS VARCHAR) IS NULL OR o.province_code = CAST(:province AS VARCHAR)
                                                             OR LOWER(o.province_name) = LOWER(CAST(:province AS VARCHAR)))
                                                ORDER BY o.created_at DESC
                                                LIMIT 500
                                                """,
                                                nativeQuery = true)
                List<OrganizationMapMarkerProjection> findMarkersInBounds(@Param("minLat") double minLat,
                                                                                                                                                                                                                                                         @Param("minLng") double minLng,
                                                                                                                                                                                                                                                         @Param("maxLat") double maxLat,
                                                                                                                                                                                                                                                         @Param("maxLng") double maxLng,
                                                                                                                                                                                                                                                         @Param("statuses") List<String> statuses,
                                                                                                                                                                                                                                                         @Param("types") List<String> types,
                        @Param("province") String province);
}
