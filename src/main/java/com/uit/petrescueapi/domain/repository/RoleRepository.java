package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Role;

import java.util.Optional;

/**
 * Domain repository contract for the Role entity.
 */
public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(Integer id);

    Optional<Role> findByCode(String code);

    void delete(Integer id);
}
