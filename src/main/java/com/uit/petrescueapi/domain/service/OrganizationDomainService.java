package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Organization;
import com.uit.petrescueapi.domain.entity.OrganizationMember;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceAlreadyExistsException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.OrganizationMemberRepository;
import com.uit.petrescueapi.domain.repository.OrganizationRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service encapsulating Organization business rules.
 *
 * Rules:
 *  • Newly created organizations always start with status {@code PENDING}.
 *  • An organization can be activated (PENDING → ACTIVE) or deactivated (ACTIVE → INACTIVE).
 *  • A user cannot be added to the same organization twice.
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrganizationDomainService {

    private final OrganizationRepository orgRepository;
    private final OrganizationMemberRepository memberRepository;
    private final VisualCodeRepository visualCodeRepository;

    // ── Commands ────────────────────────────────────

    /**
     * @deprecated Use {@link #createByUser} or {@link #createByAdmin} instead
     */
    @Deprecated
    public Organization create(Organization org) {
        log.info("Creating organization: {}", org.getName());
        org.setOrganizationId(UUID.randomUUID());
        org.setOrganizationCode(visualCodeRepository.nextOrganizationCode());
        org.setStatus(OrganizationStatus.PENDING);
        return orgRepository.save(org);
    }

    /**
     * Create organization requested by a regular user.
     * Status is set to PENDING; user becomes OWNER when approved.
     */
    public Organization createByUser(Organization org) {
        log.info("Creating organization by user: {}", org.getName());
        org.setOrganizationId(UUID.randomUUID());
        org.setOrganizationCode(visualCodeRepository.nextOrganizationCode());
        org.setStatus(OrganizationStatus.PENDING);
        return orgRepository.save(org);
    }

    /**
     * Create organization by admin.
     * Status is set directly to ACTIVE (no pending approval needed).
     */
    public Organization createByAdmin(Organization org) {
        log.info("Creating organization by admin (direct ACTIVE): {}", org.getName());
        org.setOrganizationId(UUID.randomUUID());
        org.setOrganizationCode(visualCodeRepository.nextOrganizationCode());
        org.setStatus(OrganizationStatus.ACTIVE);
        return orgRepository.save(org);
    }

    public Organization update(UUID id, Organization patch) {
        log.debug("Updating organization {}", id);
        Organization existing = findOrThrow(id);
        if (existing.getStatus() == OrganizationStatus.PENDING) {
            throw new BusinessException("Pending organizations cannot be edited");
        }
        if (patch.getName() != null)          existing.setName(patch.getName());
        if (patch.getDescription() != null)   existing.setDescription(patch.getDescription());
        if (patch.getType() != null)          existing.setType(patch.getType());
        if (patch.getStreetAddress() != null) existing.setStreetAddress(patch.getStreetAddress());
        if (patch.getWardCode() != null)      existing.setWardCode(patch.getWardCode());
        if (patch.getProvinceCode() != null)  existing.setProvinceCode(patch.getProvinceCode());
        if (patch.getPhone() != null)         existing.setPhone(patch.getPhone());
        if (patch.getEmail() != null)         existing.setEmail(patch.getEmail());
        if (patch.getImageUrl() != null)      existing.setImageUrl(patch.getImageUrl());
        if (patch.getOfficialLink() != null)  existing.setOfficialLink(patch.getOfficialLink());
        if (patch.getLatitude() != null)      existing.setLatitude(patch.getLatitude());
        if (patch.getLongitude() != null)     existing.setLongitude(patch.getLongitude());
        return orgRepository.save(existing);
    }

    public void deactivate(UUID id) {
        log.info("Deactivating organization {}", id);
        Organization org = findOrThrow(id);
        org.setStatus(OrganizationStatus.INACTIVE);
        org.setUpdatedAt(LocalDateTime.now());
        orgRepository.save(org);
    }

    public Organization changeStatus(UUID id, OrganizationStatus newStatus) {
        log.info("Changing organization {} status to {}", id, newStatus);
        Organization org = findOrThrow(id);
        OrganizationStatus oldStatus = org.getStatus();
        org.setStatus(newStatus);
        org.setUpdatedAt(LocalDateTime.now());
        Organization saved = orgRepository.save(org);

        // Auto-assign OWNER role when approving a user-requested organization
        if (oldStatus == OrganizationStatus.PENDING && newStatus == OrganizationStatus.ACTIVE) {
            UUID requestedBy = org.getRequestedByUserId();
            if (requestedBy != null && !memberRepository.exists(id, requestedBy)) {
                log.info("Auto-assigning OWNER role to user {} for approved organization {}", requestedBy, id);
                addMember(id, requestedBy, "OWNER");
            }
        }

        return saved;
    }

    public OrganizationMember addMember(UUID orgId, UUID userId, String role) {
        findOrThrow(orgId);
        if (memberRepository.exists(orgId, userId)) {
            throw new ResourceAlreadyExistsException("OrganizationMember", "userId", userId);
        }
        OrganizationMember member = OrganizationMember.builder()
                .organizationId(orgId)
                .userId(userId)
                .role(role)
                .status("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .build();
        return memberRepository.save(member);
    }

    public OrganizationMember updateMemberRole(UUID orgId, UUID userId, String role) {
        findOrThrow(orgId);
        OrganizationMember member = memberRepository.findByOrgAndUser(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationMember", "userId", userId));
        if ("OWNER".equals(member.getRole())) {
            throw new BusinessException("Cannot change role for organization OWNER", "OWNER_ROLE_IMMUTABLE");
        }
        if ("OWNER".equals(role)) {
            throw new BusinessException("Cannot assign OWNER role via this endpoint", "INVALID_ORG_ROLE");
        }
        member.setRole(role);
        memberRepository.updateRole(orgId, userId, role);
        return member;
    }

    public void removeMember(UUID orgId, UUID userId) {
        log.debug("Removing member {} from organization {}", userId, orgId);
        memberRepository.delete(orgId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID orgId, UUID userId) {
        return memberRepository.exists(orgId, userId);
    }

    @Transactional(readOnly = true)
    public Optional<String> getMemberRole(UUID orgId, UUID userId) {
        return memberRepository.findRoleByOrgAndUser(orgId, userId);
    }

    /**
     * Check if a user is an owner of the organization.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(UUID orgId, UUID userId) {
        return memberRepository.findRoleByOrgAndUser(orgId, userId)
                .map("OWNER"::equals)
                .orElse(false);
    }

    // ── Helpers ──────────────────────────────────────

    @Transactional(readOnly = true)
    private Organization findOrThrow(UUID id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }
}
