package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.OrganizationMember;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for OrganizationMember aggregate.
 */
public interface OrganizationMemberRepository {
    OrganizationMember save(OrganizationMember member);
    void delete(UUID organizationId, UUID userId);
    boolean exists(UUID organizationId, UUID userId);
    Optional<OrganizationMember> findByOrgAndUser(UUID organizationId, UUID userId);
    UUID findOrganizationIdByUserId(UUID userId);
    String findOrgRoleByUserId(UUID userId);
    Optional<String> findRoleByOrgAndUser(UUID organizationId, UUID userId);
    void updateRole(UUID organizationId, UUID userId, String role);
}
