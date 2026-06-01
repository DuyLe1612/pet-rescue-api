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
    Optional<RescueCaseCompletionJpaEntity> findByCaseIdAndDeletedFalse(UUID caseId);
    Page<RescueCaseCompletionJpaEntity> findByCaseIdAndDeletedFalse(UUID caseId, Pageable pageable);
    Page<RescueCaseCompletionJpaEntity> findByDeletedFalse(Pageable pageable);
}
