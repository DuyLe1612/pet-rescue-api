package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.organization.AddMemberRequestDto;
import com.uit.petrescueapi.application.dto.organization.CreateOrganizationRequestDto;
import com.uit.petrescueapi.application.port.command.OrganizationCommandPort;
import com.uit.petrescueapi.domain.entity.Organization;
import com.uit.petrescueapi.domain.entity.OrganizationMember;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command (write) use-case for Organization operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link OrganizationDomainService}.
 * 
 * Note: Request DTOs contain codes (provinceCode, wardCode). Names can be
 * resolved from a location service if needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationCommandUseCase implements OrganizationCommandPort {

    private final OrganizationDomainService domainService;

    @Override
    public Organization create(CreateOrganizationRequestDto cmd) {
        log.debug("Command: create organization '{}'", cmd.getName());
        Organization org = buildOrganization(cmd);
        return domainService.create(org);
    }

    @Override
    public Organization createByUser(CreateOrganizationRequestDto cmd, UUID requestedByUserId) {
        log.debug("Command: create organization '{}' by user {}", cmd.getName(), requestedByUserId);
        Organization org = buildOrganization(cmd);
        org.setRequestedByUserId(requestedByUserId);
        return domainService.createByUser(org);
    }

    @Override
    public Organization createByAdmin(CreateOrganizationRequestDto cmd) {
        log.debug("Command: create organization '{}' by admin (direct ACTIVE)", cmd.getName());
        Organization org = buildOrganization(cmd);
        return domainService.createByAdmin(org);
    }

    @Override
    public Organization update(UUID id, CreateOrganizationRequestDto cmd) {
        log.debug("Command: update organization {}", id);
        Organization patch = buildOrganization(cmd);
        return domainService.update(id, patch);
    }

    @Override
    public void deactivate(UUID id) {
        log.debug("Command: deactivate organization {}", id);
        domainService.deactivate(id);
    }

    @Override
    public Organization changeStatus(UUID id, OrganizationStatus newStatus) {
        log.debug("Command: change organization {} status to {}", id, newStatus);
        return domainService.changeStatus(id, newStatus);
    }

    @Override
    public OrganizationMember addMember(UUID organizationId, AddMemberRequestDto cmd) {
        log.debug("Command: add member {} to organization {}", cmd.getUserId(), organizationId);
        return domainService.addMember(organizationId, cmd.getUserId(), cmd.getRole());
    }

    @Override
    public OrganizationMember updateMemberRole(UUID organizationId, UUID userId, String role) {
        log.debug("Command: update member {} role in organization {}", userId, organizationId);
        return domainService.updateMemberRole(organizationId, userId, role);
    }

    @Override
    public void removeMember(UUID organizationId, UUID userId) {
        log.debug("Command: remove member {} from organization {}", userId, organizationId);
        domainService.removeMember(organizationId, userId);
    }

    /**
     * Build Organization from request DTO.
     * Request contains both codes and names for location fields.
     */
    private Organization buildOrganization(CreateOrganizationRequestDto cmd) {
        return Organization.builder()
                .name(cmd.getName())
                .description(cmd.getDescription())
                .type(cmd.getType())
                .streetAddress(cmd.getStreetAddress())
                .wardCode(cmd.getWardCode())
                .wardName(cmd.getWardName())
                .provinceCode(cmd.getProvinceCode())
                .provinceName(cmd.getProvinceName())
                .phone(cmd.getPhone())
                .email(cmd.getEmail())
                .imageUrl(cmd.getImageUrl())
                .officialLink(cmd.getOfficialLink())
                .latitude(cmd.getLatitude())
                .longitude(cmd.getLongitude())
                .build();
    }
}
