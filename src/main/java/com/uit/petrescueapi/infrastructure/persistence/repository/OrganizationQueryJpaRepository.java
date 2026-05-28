package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationDetailProjection;
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
                   o.ward_name            AS wardName,
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
                                                                         o.ward_name AS wardName,
                                                                         o.province_name AS provinceName
                                                FROM organizations o
                                                WHERE o.is_deleted = false
                                                        AND o.location IS NOT NULL
                                                        AND o.status IN (:statuses)
                                                        AND o.type IN (:types)
                                                ORDER BY o.created_at DESC
                                                LIMIT 500
                                                """, nativeQuery = true)
                List<OrganizationMapMarkerProjection> findMapMarkers(@Param("statuses") List<String> statuses,
                                                                                                                                                                                                                                 @Param("types") List<String> types);

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
                                                        AND ST_Y(o.location) BETWEEN :minLat AND :maxLat
                                                        AND ST_X(o.location) BETWEEN :minLng AND :maxLng
                                                        AND o.status IN (:statuses)
                                                        AND (:search IS NULL OR :search = '' OR
                                                             LOWER(o.organization_code) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.type) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.province_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                                                             LOWER(o.ward_name) LIKE LOWER(CONCAT('%', :search, '%')))
                                                ORDER BY o.created_at DESC
                                                """,
                                                countQuery = "SELECT COUNT(1) FROM organizations o WHERE o.is_deleted = false AND o.location IS NOT NULL AND ST_Y(o.location) BETWEEN :minLat AND :maxLat AND ST_X(o.location) BETWEEN :minLng AND :maxLng AND o.status IN (:statuses) AND (:search IS NULL OR :search = '' OR LOWER(o.organization_code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.type) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.province_name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.ward_name) LIKE LOWER(CONCAT('%', :search, '%'))) ",
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
                                                                         o.ward_name AS wardName,
                                                                         o.province_name AS provinceName
                                                FROM organizations o
                                                WHERE o.is_deleted = false
                                                        AND o.location IS NOT NULL
                                                        AND ST_Y(o.location) BETWEEN :minLat AND :maxLat
                                                        AND ST_X(o.location) BETWEEN :minLng AND :maxLng
                                                        AND o.status IN (:statuses)
                                                        AND o.type IN (:types)
                                                ORDER BY o.created_at DESC
                                                LIMIT 500
                                                """,
                                                nativeQuery = true)
                List<OrganizationMapMarkerProjection> findMarkersInBounds(@Param("minLat") double minLat,
                                                                                                                                                                                                                                                         @Param("minLng") double minLng,
                                                                                                                                                                                                                                                         @Param("maxLat") double maxLat,
                                                                                                                                                                                                                                                         @Param("maxLng") double maxLng,
                                                                                                                                                                                                                                                         @Param("statuses") List<String> statuses,
                                                                                                                                                                                                                                                         @Param("types") List<String> types);
}
