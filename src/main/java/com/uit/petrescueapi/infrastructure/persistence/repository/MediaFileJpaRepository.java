package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.MediaFileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link MediaFileJpaEntity}.
 * Used by the <b>command-side</b> adapter for media file management.
 */
@Repository
public interface MediaFileJpaRepository extends JpaRepository<MediaFileJpaEntity, UUID> {

    Optional<MediaFileJpaEntity> findByMediaIdAndDeletedFalse(UUID mediaId);

    Optional<MediaFileJpaEntity> findByPublicIdAndDeletedFalse(String publicId);
}
