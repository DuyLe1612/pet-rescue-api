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

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT a.application_id
            FROM adoption_applications a
            WHERE a.is_deleted = FALSE
              AND a.status IN ('PENDING', 'APPROVED')
              AND a.match_level IS NULL
              AND EXISTS (SELECT 1 FROM adopter_profiles p WHERE p.user_id = a.applicant_id)
              AND EXISTS (SELECT 1 FROM pet_traits t WHERE t.pet_id = a.pet_id)
            ORDER BY a.created_at
            LIMIT :limit
            """, nativeQuery = true)
    java.util.List<UUID> findOpenWithoutMatch(@org.springframework.data.repository.query.Param("limit") int limit);

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT a.applicantId FROM AdoptionApplicationJpaEntity a
            WHERE a.petId = :petId AND a.applicationId <> :exceptId AND a.deleted = false
              AND a.status IN (com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus.PENDING,
                               com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus.APPROVED)
            """)
    java.util.List<UUID> findOpenApplicantIdsForPet(@org.springframework.data.repository.query.Param("petId") UUID petId,
                                                    @org.springframework.data.repository.query.Param("exceptId") UUID exceptId);

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
