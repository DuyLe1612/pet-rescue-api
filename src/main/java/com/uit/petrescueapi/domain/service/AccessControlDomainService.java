package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.OrganizationMemberRepository;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Resource-level authorization rules that URL matching cannot express:
 * who may act on a specific rescue case, adoption application or pet.
 *
 * <p>A {@code null} requester is an anonymous caller. It is rejected with 403, not 401,
 * because the web client clears its session on any 401.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessControlDomainService {

    private static final String ADMIN = "ADMIN";
    private static final Set<String> CASE_HANDLER_ROLES = Set.of("OWNER", "STAFF");
    private static final Set<String> ADOPTION_DECIDER_ROLES = Set.of("OWNER", "STAFF");
    private static final Set<String> PET_MANAGER_ROLES = Set.of("OWNER", "STAFF", "VET");

    private final UserDomainService userDomainService;
    private final OrganizationDomainService organizationDomainService;
    private final OrganizationMemberRepository memberRepository;
    private final PetCurrentOwnerRepository currentOwnerRepository;

    public boolean isAdmin(UUID userId) {
        return userId != null && userDomainService.hasRole(userId, ADMIN);
    }

    // ── Generic ─────────────────────────────────────

    public void requireAdmin(UUID requesterId) {
        if (!isAdmin(requesterId)) {
            throw new ForbiddenException("Only administrators can perform this action");
        }
    }

    /** Passes for the resource's own user (author, uploader, applicant) and for admins. */
    public void requireSelfOrAdmin(UUID ownerUserId, UUID requesterId) {
        if (requesterId != null && requesterId.equals(ownerUserId)) {
            return;
        }
        if (!isAdmin(requesterId)) {
            throw new ForbiddenException("You are not allowed to access this resource");
        }
    }

    public void requireOrgMemberOrAdmin(UUID organizationId, UUID requesterId) {
        if (!isAdmin(requesterId) && !isOrgMember(organizationId, requesterId)) {
            throw new ForbiddenException("You are not a member of this organization");
        }
    }

    // ── Rescue cases ────────────────────────────────

    /** Editing case details: admin, the reporter, or the handling organization's staff. */
    public void requireCaseEditor(RescueCase rescueCase, UUID requesterId) {
        if (requesterId != null && requesterId.equals(rescueCase.getReportedBy())) {
            return;
        }
        if (!canHandleCase(rescueCase, requesterId)) {
            throw new ForbiddenException("You are not allowed to manage this rescue case");
        }
    }

    /**
     * Changing status: a reporter who is not a handler may only close their own report.
     * Otherwise they could mark it RESCUED and skip completion approval.
     */
    public void requireCaseStatusChange(RescueCase rescueCase, RescueCaseStatus newStatus, UUID requesterId) {
        if (canHandleCase(rescueCase, requesterId)) {
            return;
        }
        boolean reporterClosing = requesterId != null
                && requesterId.equals(rescueCase.getReportedBy())
                && newStatus == RescueCaseStatus.CLOSED;
        if (!reporterClosing) {
            throw new ForbiddenException("You are not allowed to change the status of this rescue case");
        }
    }

    /** Approving a completion: handlers only, and a non-admin reporter cannot approve their own case. */
    public void requireCompletionApprover(RescueCase rescueCase, UUID requesterId) {
        if (isAdmin(requesterId)) {
            return;
        }
        if (requesterId != null && requesterId.equals(rescueCase.getReportedBy())) {
            throw new ForbiddenException("The reporter cannot approve the completion of their own case");
        }
        if (!canHandleCase(rescueCase, requesterId)) {
            throw new ForbiddenException("You are not allowed to approve this rescue completion");
        }
    }

    /**
     * Cases have no assigned organization yet, so an unassigned case can be handled by
     * OWNER or STAFF of any organization. An assigned case is limited to that organization.
     */
    private boolean canHandleCase(RescueCase rescueCase, UUID requesterId) {
        if (isAdmin(requesterId)) {
            return true;
        }
        if (requesterId == null) {
            return false;
        }
        if (rescueCase.getOrganizationId() != null) {
            return hasOrgRole(rescueCase.getOrganizationId(), requesterId, CASE_HANDLER_ROLES);
        }
        String role = memberRepository.findOrgRoleByUserId(requesterId);
        return role != null && CASE_HANDLER_ROLES.contains(role);
    }

    // ── Adoption applications ───────────────────────

    public void requireAdoptionDecider(UUID organizationId, UUID requesterId) {
        if (!canDecideAdoption(organizationId, requesterId)) {
            throw new ForbiddenException("You are not allowed to decide this adoption application");
        }
    }

    public void requireAdoptionCanceler(UUID applicantId, UUID organizationId, UUID requesterId) {
        boolean applicant = requesterId != null && requesterId.equals(applicantId);
        if (!applicant && !canDecideAdoption(organizationId, requesterId)) {
            throw new ForbiddenException("You are not allowed to cancel this adoption application");
        }
    }

    public void requireAdoptionViewer(UUID applicantId, UUID organizationId, UUID requesterId) {
        boolean applicant = requesterId != null && requesterId.equals(applicantId);
        if (!applicant && !isAdmin(requesterId) && !isOrgMember(organizationId, requesterId)) {
            throw new ForbiddenException("You are not allowed to view this adoption application");
        }
    }

    private boolean canDecideAdoption(UUID organizationId, UUID requesterId) {
        return isAdmin(requesterId)
                || (requesterId != null && hasOrgRole(organizationId, requesterId, ADOPTION_DECIDER_ROLES));
    }

    // ── Pets ────────────────────────────────────────

    public void requirePetManager(UUID petId, UUID requesterId) {
        if (!canManagePet(petId, requesterId)) {
            throw new ForbiddenException("You are not allowed to manage this pet");
        }
    }

    /**
     * Medical and ownership history: public for organization-held pets, because adopters
     * need it before applying. Private for pets owned by an individual.
     */
    public void requirePetRecordsReader(UUID petId, UUID requesterId) {
        boolean orgHeld = currentOwnerRepository.findByPetId(petId)
                .map(owner -> "ORGANIZATION".equals(owner.getOwnerType()))
                .orElse(false);
        if (!orgHeld && !canManagePet(petId, requesterId)) {
            throw new ForbiddenException("The records of this pet are private");
        }
    }

    private boolean canManagePet(UUID petId, UUID requesterId) {
        if (requesterId == null) {
            return false;
        }
        if (isAdmin(requesterId)) {
            return true;
        }
        return currentOwnerRepository.findByPetId(petId)
                .map(owner -> isPetManager(owner, requesterId))
                .orElse(false);
    }

    private boolean isPetManager(PetCurrentOwner owner, UUID requesterId) {
        if ("USER".equals(owner.getOwnerType())) {
            return requesterId.equals(owner.getOwnerId());
        }
        return "ORGANIZATION".equals(owner.getOwnerType())
                && (requesterId.equals(owner.getCaretakerUserId())
                || hasOrgRole(owner.getOwnerId(), requesterId, PET_MANAGER_ROLES));
    }

    // ── Organization helpers ────────────────────────

    private boolean isOrgMember(UUID organizationId, UUID requesterId) {
        return organizationId != null
                && requesterId != null
                && organizationDomainService.getMemberRole(organizationId, requesterId).isPresent();
    }

    private boolean hasOrgRole(UUID organizationId, UUID requesterId, Set<String> roles) {
        return organizationId != null
                && organizationDomainService.getMemberRole(organizationId, requesterId)
                        .filter(roles::contains)
                        .isPresent();
    }
}
