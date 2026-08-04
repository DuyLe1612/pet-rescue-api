package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.repository.AdoptionApplicationRepository;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.infrastructure.persistence.mapper.AdoptionApplicationEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.AdoptionApplicationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdoptionApplicationRepositoryAdapter implements AdoptionApplicationRepository {

    private final AdoptionApplicationJpaRepository jpa;
    private final AdoptionApplicationEntityMapper mapper;

    @Override
    public AdoptionApplication save(AdoptionApplication application) {
        return mapper.toDomain(jpa.save(mapper.toEntity(application)));
    }

    @Override
    public Optional<AdoptionApplication> findById(UUID applicationId) {
        return jpa.findById(applicationId)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Page<AdoptionApplication> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<AdoptionApplication> findByStatus(AdoptionApplicationStatus status, Pageable pageable) {
        // Filter by status in-memory; can be replaced with a dedicated JPA query later
        return jpa.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public long countByApplicantIdAndStatus(UUID applicantId, AdoptionApplicationStatus status) {
        return jpa.countByApplicantIdAndStatusAndDeletedFalse(applicantId, status);
    }
}
