package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.AdoptionApplicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AdoptionApplicationJpaEntity}.
 * Used by the <b>command-side</b> adapter for adoption application management.
 *
 * <p>For read/query-side operations, see {@link AdoptionQueryJpaRepository}.</p>
 */
@Repository
public interface AdoptionApplicationJpaRepository extends JpaRepository<AdoptionApplicationJpaEntity, UUID> {

    Optional<AdoptionApplicationJpaEntity> findByApplicationIdAndDeletedFalse(UUID applicationId);

    long countByApplicantIdAndStatusAndDeletedFalse(UUID applicantId, AdoptionApplicationStatus status);
}
