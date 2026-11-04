package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RescueCaseJpaEntity}.
 * Used by the <b>command-side</b> adapter for rescue case management.
 *
 * <p>For read/query-side operations, see {@link RescueCaseQueryJpaRepository}.</p>
 */
@Repository
public interface RescueCaseJpaRepository extends JpaRepository<RescueCaseJpaEntity, UUID> {

    Optional<RescueCaseJpaEntity> findByCaseIdAndDeletedFalse(UUID caseId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM RescueCaseJpaEntity c WHERE c.caseId = :caseId AND c.deleted = false")
    Optional<RescueCaseJpaEntity> findForUpdate(@org.springframework.data.repository.query.Param("caseId") UUID caseId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE RescueCaseJpaEntity c
            SET c.organizationId = :organizationId, c.version = c.version + 1
            WHERE c.caseId = :caseId AND c.organizationId IS NULL
            """)
    int assignOrganization(@org.springframework.data.repository.query.Param("caseId") UUID caseId,
                           @org.springframework.data.repository.query.Param("organizationId") UUID organizationId);
}
