package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.ReclaimLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReclaimLogJpaRepository extends JpaRepository<ReclaimLogJpaEntity, UUID> {
    Page<ReclaimLogJpaEntity> findByDeletedFalse(Pageable pageable);
    Page<ReclaimLogJpaEntity> findByOrganizationIdAndDeletedFalse(UUID organizationId, Pageable pageable);
}
