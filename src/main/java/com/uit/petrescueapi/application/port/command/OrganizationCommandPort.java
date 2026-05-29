package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.organization.AddMemberRequestDto;
import com.uit.petrescueapi.application.dto.organization.CreateOrganizationRequestDto;
import com.uit.petrescueapi.domain.entity.Organization;
import com.uit.petrescueapi.domain.entity.OrganizationMember;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;

import java.util.UUID;

/**
 * Command (write) port for Organization operations.
 * Returns domain entities — the controller maps to DTOs (same pattern as PetCommandPort).
 */
public interface OrganizationCommandPort {

    /**
     * @deprecated Use {@link #createByUser} or {@link #createByAdmin} instead
     */
    @Deprecated
    Organization create(CreateOrganizationRequestDto cmd);

    /**
     * Create organization requested by a regular user.
     * Status is set to PENDING; user becomes OWNER when approved.
     */
    Organization createByUser(CreateOrganizationRequestDto cmd, UUID requestedByUserId);

    /**
     * Create organization by admin.
     * Status is set directly to ACTIVE.
     */
    Organization createByAdmin(CreateOrganizationRequestDto cmd);

    Organization update(UUID id, CreateOrganizationRequestDto cmd);

    void deactivate(UUID id);

    Organization changeStatus(UUID id, OrganizationStatus newStatus);

    OrganizationMember addMember(UUID organizationId, AddMemberRequestDto cmd);

    OrganizationMember updateMemberRole(UUID organizationId, UUID userId, String role);

    void removeMember(UUID organizationId, UUID userId);
}
