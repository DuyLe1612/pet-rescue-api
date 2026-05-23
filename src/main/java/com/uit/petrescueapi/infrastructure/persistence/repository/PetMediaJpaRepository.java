package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetMediaJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetMediaProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PetMediaJpaEntity}.
 * Used by the <b>command-side</b> adapter for pet media management.
 */
@Repository
public interface PetMediaJpaRepository extends JpaRepository<PetMediaJpaEntity, UUID> {

    List<PetMediaJpaEntity> findAllByPetId(UUID petId);

    /**
     * Optimized paginated query that JOINs pet_media with media_files in ONE query.
     * Avoids N+1 problem by fetching public_id for URL generation in the same query.
     *
     * <p>Performance: 1 query instead of N+1 queries!</p>
     */
    @Query(value = """
            SELECT pm.media_id      AS mediaId,
                   pm.pet_id        AS petId,
                   pm.media_file_id AS mediaFileId,
                   pm.type          AS type,
                 pm.is_primary    AS primaryMedia,
                   pm.created_at    AS createdAt,
                   mf.public_id     AS publicId
            FROM pet_media pm
            LEFT JOIN media_files mf ON pm.media_file_id = mf.media_id
            WHERE pm.pet_id = :petId
            ORDER BY pm.created_at DESC
            """,
            countQuery = "SELECT COUNT(*) FROM pet_media WHERE pet_id = :petId",
            nativeQuery = true)
    Page<PetMediaProjection> findMediaWithFilesByPetId(@Param("petId") UUID petId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM PetMediaJpaEntity pm WHERE pm.petId = :petId")
    @Transactional
    void deleteAllByPetId(@Param("petId") UUID petId);

    List<PetMediaJpaEntity> findByPetIdAndMediaFileId(UUID petId, UUID mediaFileId);

    @Modifying
    @Query(value = "UPDATE pet_media SET is_primary = FALSE WHERE pet_id = :petId", nativeQuery = true)
    @Transactional
    void clearPrimaryByPetId(@Param("petId") UUID petId);

    @Modifying
    @Query(value = "UPDATE pet_media SET is_primary = TRUE WHERE pet_id = :petId AND media_id = :mediaId", nativeQuery = true)
    @Transactional
    void setPrimaryByPetIdAndMediaId(@Param("petId") UUID petId, @Param("mediaId") UUID mediaId);

    @Modifying
    @Query(value = "DELETE FROM pet_media WHERE pet_id = :petId AND media_id = :mediaId", nativeQuery = true)
    @Transactional
    void deleteByPetIdAndMediaId(@Param("petId") UUID petId, @Param("mediaId") UUID mediaId);

    @Query(value = "SELECT * FROM pet_media WHERE pet_id = :petId AND is_primary = TRUE LIMIT 1", nativeQuery = true)
    java.util.Optional<PetMediaJpaEntity> findPrimaryByPetId(@Param("petId") UUID petId);
}
