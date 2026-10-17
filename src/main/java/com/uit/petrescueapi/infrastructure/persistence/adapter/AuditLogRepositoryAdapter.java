package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.AuditLog;
import com.uit.petrescueapi.domain.repository.AuditLogRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.AuditLogEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;
    private final AuditLogEntityMapper mapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return mapper.toDomain(jpa.save(mapper.toEntity(auditLog)));
    }
}
