package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseCompletionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RescueCaseCompletionJpaRepository extends JpaRepository<RescueCaseCompletionJpaEntity, UUID> {
    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM RescueCaseCompletionJpaEntity c
            WHERE c.caseId = :caseId AND c.deleted = false AND c.status <> 'REJECTED'
            """)
    Optional<RescueCaseCompletionJpaEntity> findOpenByCaseId(@org.springframework.data.repository.query.Param("caseId") UUID caseId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM RescueCaseCompletionJpaEntity c
            WHERE c.status = 'PENDING' AND c.escalatedAt IS NULL AND c.deleted = false AND c.createdAt <= :before
            ORDER BY c.createdAt
            """)
    java.util.List<RescueCaseCompletionJpaEntity> findPendingToEscalate(
            @org.springframework.data.repository.query.Param("before") java.time.LocalDateTime before, Pageable pageable);
    Page<RescueCaseCompletionJpaEntity> findByCaseIdAndDeletedFalse(UUID caseId, Pageable pageable);
    Page<RescueCaseCompletionJpaEntity> findByDeletedFalse(Pageable pageable);
}
