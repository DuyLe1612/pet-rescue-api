package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.TagJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TagJpaEntity}.
 * Used by the <b>command-side</b> adapter for tag management.
 *
 * <p>For read/query-side operations, see {@link TagQueryJpaRepository}.</p>
 */
@Repository
public interface TagJpaRepository extends JpaRepository<TagJpaEntity, UUID> {

    Optional<TagJpaEntity> findByCodeAndDeletedFalse(String code);
}
