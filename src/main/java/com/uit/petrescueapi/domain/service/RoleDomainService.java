package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Role;
import com.uit.petrescueapi.domain.exception.ResourceAlreadyExistsException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Domain service encapsulating Role business rules.
 *
 * Rules:
 *  - Role codes must be unique.
 *  - Deletion is soft-delete (marks as inactive).
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleDomainService {

    private final RoleRepository roleRepository;

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Role findById(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
    }

    // ── Commands ────────────────────────────────────

    /**
     * Create a new role.
     * Validates that the role code is unique.
     */
    public Role create(Role role) {
        log.info("Creating role with code '{}'", role.getCode());
        roleRepository.findByCode(role.getCode()).ifPresent(existing -> {
            throw new ResourceAlreadyExistsException("Role", "code", role.getCode());
        });

        role.setCreatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    /**
     * Soft delete a role.
     */
    public void delete(Integer id) {
        log.info("Deleting role {}", id);
        roleRepository.delete(id);
    }

    // ── Private helpers ─────────────────────────────
}
