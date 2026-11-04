package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.repository.RescueCaseRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueCaseEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RescueCaseRepositoryAdapter implements RescueCaseRepository {

    private final RescueCaseJpaRepository jpa;
    private final RescueCaseEntityMapper mapper;

    @Override
    public RescueCase save(RescueCase rescueCase) {
        return mapper.toDomain(jpa.save(mapper.toEntity(rescueCase)));
    }

    @Override
    public Optional<RescueCase> findByIdForUpdate(UUID caseId) {
        return jpa.findForUpdate(caseId).map(mapper::toDomain);
    }

    @Override
    public boolean assignOrganization(UUID caseId, UUID organizationId) {
        return jpa.assignOrganization(caseId, organizationId) > 0;
    }

    @Override
    public Optional<RescueCase> findById(UUID caseId) {
        return jpa.findById(caseId)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Page<RescueCase> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }
}
