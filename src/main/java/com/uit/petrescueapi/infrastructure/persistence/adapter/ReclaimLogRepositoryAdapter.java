package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.ReclaimLog;
import com.uit.petrescueapi.domain.repository.ReclaimLogRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.ReclaimLogEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.ReclaimLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReclaimLogRepositoryAdapter implements ReclaimLogRepository {
    private final ReclaimLogJpaRepository jpa;
    private final ReclaimLogEntityMapper mapper;

    @Override
    public ReclaimLog save(ReclaimLog reclaimLog) {
        return mapper.toDomain(jpa.save(mapper.toEntity(reclaimLog)));
    }

    @Override
    public Page<ReclaimLog> findAll(Pageable pageable) {
        return jpa.findByDeletedFalse(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<ReclaimLog> findByOrganizationId(UUID organizationId, Pageable pageable) {
        return jpa.findByOrganizationIdAndDeletedFalse(organizationId, pageable).map(mapper::toDomain);
    }
}
