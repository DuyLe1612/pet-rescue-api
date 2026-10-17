package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Role;
import com.uit.petrescueapi.domain.repository.RoleRepository;
import com.uit.petrescueapi.infrastructure.persistence.entity.RoleJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RoleEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpa;
    private final RoleEntityMapper mapper;

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.toEntity(role);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Role> findById(Integer id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpa.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public void delete(Integer id) {
        jpa.deleteById(id);
    }
}
