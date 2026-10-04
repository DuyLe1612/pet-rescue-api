package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.UserGeoLocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGeoLocationJpaRepository extends JpaRepository<UserGeoLocationJpaEntity, UUID> {

    Optional<UserGeoLocationJpaEntity> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE UserGeoLocationJpaEntity g SET g.active = false WHERE g.userId = :userId")
    void markInactive(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserGeoLocationJpaEntity g SET g.active = false WHERE g.active = true AND g.lastSeenAt < :threshold")
    int markInactiveByLastSeenBefore(@Param("threshold") OffsetDateTime threshold);

    @Query(value = """
            SELECT g.*
            FROM user_geo_locations g
            WHERE g.user_id <> :userId
              AND g.is_active = true
              AND (
                  6371 * acos(
                      cos(radians(:lat)) * cos(radians(g.lat)) * cos(radians(g.lng) - radians(:lng))
                      + sin(radians(:lat)) * sin(radians(g.lat))
                  )
              ) <= :radiusKm
            ORDER BY g.last_seen_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UserGeoLocationJpaEntity> findNearbyActive(
            @Param("userId") UUID userId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("limit") int limit
    );
}
