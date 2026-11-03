package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseClaimJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescueCaseClaimJpaRepository extends JpaRepository<RescueCaseClaimJpaEntity, UUID> {

    List<RescueCaseClaimJpaEntity> findByCaseIdAndStatusOrderByClaimedAtAsc(UUID caseId, String status);

    long countByCaseIdAndStatus(UUID caseId, String status);

    Optional<RescueCaseClaimJpaEntity> findFirstByCaseIdAndUserIdAndStatus(UUID caseId, UUID userId, String status);

    Optional<RescueCaseClaimJpaEntity> findFirstByCaseIdAndOrganizationIdAndStatus(UUID caseId, UUID organizationId, String status);

    List<RescueCaseClaimJpaEntity> findByUserIdAndStatusOrderByClaimedAtDesc(UUID userId, String status);

    long countByUserIdAndStatusAndOrganizationIdIsNull(UUID userId, String status);

    @Query("""
            SELECT c FROM RescueCaseClaimJpaEntity c
            WHERE c.status = 'ACTIVE' AND c.expiresAt IS NOT NULL AND c.expiresAt <= :time
            ORDER BY c.expiresAt
            """)
    List<RescueCaseClaimJpaEntity> findExpiredBefore(@Param("time") LocalDateTime time, Pageable pageable);

    @Query("""
            SELECT c FROM RescueCaseClaimJpaEntity c
            WHERE c.status = 'ACTIVE' AND c.remindedAt IS NULL
              AND c.expiresAt IS NOT NULL AND c.expiresAt <= :time
            ORDER BY c.expiresAt
            """)
    List<RescueCaseClaimJpaEntity> findToRemindBefore(@Param("time") LocalDateTime time, Pageable pageable);
}
