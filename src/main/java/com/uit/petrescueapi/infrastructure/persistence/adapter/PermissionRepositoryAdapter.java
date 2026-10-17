package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.repository.PermissionRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PermissionEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpa;
    private final PermissionEntityMapper mapper;
}
