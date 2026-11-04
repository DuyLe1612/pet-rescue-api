package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.AdoptionApplicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AdoptionApplicationJpaEntity}.
 * Used by the <b>command-side</b> adapter for adoption application management.
 *
 * <p>For read/query-side operations, see {@link AdoptionQueryJpaRepository}.</p>
 */
@Repository
public interface AdoptionApplicationJpaRepository extends JpaRepository<AdoptionApplicationJpaEntity, UUID> {

    long countByApplicantIdAndStatusAndDeletedFalse(UUID applicantId, AdoptionApplicationStatus status);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE AdoptionApplicationJpaEntity a
            SET a.status = com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus.CANCELED,
                a.rejectReason = :reason, a.updatedAt = :now
            WHERE a.petId = :petId AND a.applicationId <> :exceptId AND a.deleted = false
              AND a.status IN (com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus.PENDING,
                               com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus.APPROVED)
            """)
    int cancelOpenForPet(@org.springframework.data.repository.query.Param("petId") UUID petId,
                         @org.springframework.data.repository.query.Param("exceptId") UUID exceptId,
                         @org.springframework.data.repository.query.Param("reason") String reason,
                         @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
