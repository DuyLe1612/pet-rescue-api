package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Organization;
import com.uit.petrescueapi.domain.repository.OrganizationRepository;
import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.mapper.OrganizationEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.OrganizationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationJpaRepository jpa;
    private final OrganizationEntityMapper mapper;

    @Override
    public Organization save(Organization org) {
        return mapper.toDomain(jpa.save(mapper.toEntity(org)));
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Page<Organization> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }
}
