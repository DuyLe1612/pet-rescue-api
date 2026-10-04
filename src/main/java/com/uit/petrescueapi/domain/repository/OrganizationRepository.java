package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {
    Organization save(Organization org);
    Optional<Organization> findById(UUID id);
    void delete(UUID id);
    Page<Organization> findAll(Pageable pageable);
}
