package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.OrganizationMemberRepository;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessControlDomainServiceTest {

    final UserDomainService users = mock(UserDomainService.class);
    final OrganizationDomainService orgs = mock(OrganizationDomainService.class);
    final OrganizationMemberRepository members = mock(OrganizationMemberRepository.class);
    final PetCurrentOwnerRepository owners = mock(PetCurrentOwnerRepository.class);
    final AccessControlDomainService access = new AccessControlDomainService(users, orgs, members, owners);

    final UUID requester = UUID.randomUUID();
    final UUID reporter = UUID.randomUUID();
    final UUID orgId = UUID.randomUUID();
    final UUID petId = UUID.randomUUID();

    private RescueCase unassignedCase() {
        return RescueCase.builder().reportedBy(reporter).build();
    }

    private RescueCase assignedCase() {
        return RescueCase.builder().reportedBy(reporter).organizationId(orgId).build();
    }

    private void requesterIsAdmin() {
        when(users.hasRole(requester, "ADMIN")).thenReturn(true);
    }

    private void requesterHasRoleInOrg(String role) {
        when(orgs.getMemberRole(orgId, requester)).thenReturn(Optional.of(role));
    }

    private void petOwnedBy(String type, UUID ownerId) {
        when(owners.findByPetId(petId)).thenReturn(Optional.of(
                PetCurrentOwner.builder().petId(petId).ownerType(type).ownerId(ownerId).build()));
    }

    // ── Rescue cases ────────────────────────────────

    @Test
    void reporterCanEditOwnCase() {
        assertThatCode(() -> access.requireCaseEditor(unassignedCase(), reporter)).doesNotThrowAnyException();
    }

    @Test
    void strangerCannotEditCase() {
        assertThatThrownBy(() -> access.requireCaseEditor(unassignedCase(), requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void anyOrgStaffCanHandleUnassignedCase() {
        when(members.findOrgRoleByUserId(requester)).thenReturn("STAFF");
        assertThatCode(() -> access.requireCaseEditor(unassignedCase(), requester)).doesNotThrowAnyException();
    }

    @Test
    void vetCannotHandleUnassignedCase() {
        when(members.findOrgRoleByUserId(requester)).thenReturn("VET");
        assertThatThrownBy(() -> access.requireCaseEditor(unassignedCase(), requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void staffOfAnotherOrgCannotHandleAssignedCase() {
        when(members.findOrgRoleByUserId(requester)).thenReturn("STAFF");
        when(orgs.getMemberRole(orgId, requester)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireCaseEditor(assignedCase(), requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void onlyAdminsMoveStatusFreely() {
        requesterHasRoleInOrg("STAFF");
        assertThatThrownBy(() -> access.requireCaseStatusChange(assignedCase(), RescueCaseStatus.RESCUED, requester))
                .isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> access.requireCaseStatusChange(assignedCase(), RescueCaseStatus.CLOSED, requester))
                .doesNotThrowAnyException();
        requesterIsAdmin();
        assertThatCode(() -> access.requireCaseStatusChange(assignedCase(), RescueCaseStatus.RESCUED, requester))
                .doesNotThrowAnyException();
    }

    @Test
    void nobodyApprovesTheirOwnReport() {
        requesterHasRoleInOrg("STAFF");
        assertThatThrownBy(() -> access.requireCompletionApprover(assignedCase(), requester, requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void contactIsForPeopleWhoNeedIt() {
        assertThat(access.canViewCaseContact(unassignedCase(), false, null)).isFalse();
        assertThat(access.canViewCaseContact(unassignedCase(), false, requester)).isFalse();
        assertThat(access.canViewCaseContact(unassignedCase(), true, requester)).isTrue();
        assertThat(access.canViewCaseContact(unassignedCase(), false, reporter)).isTrue();
        requesterHasRoleInOrg("STAFF");
        assertThat(access.canViewCaseContact(assignedCase(), false, requester)).isTrue();
    }

    @Test
    void reporterCanCloseButNotMarkOwnCaseRescued() {
        assertThatCode(() -> access.requireCaseStatusChange(unassignedCase(), RescueCaseStatus.CLOSED, reporter))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireCaseStatusChange(unassignedCase(), RescueCaseStatus.RESCUED, reporter))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void reporterCannotApproveOwnCompletionEvenAsStaff() {
        when(members.findOrgRoleByUserId(reporter)).thenReturn("OWNER");
        assertThatThrownBy(() -> access.requireCompletionApprover(unassignedCase(), UUID.randomUUID(), reporter))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void orgStaffCanApproveCompletion() {
        requesterHasRoleInOrg("STAFF");
        assertThatCode(() -> access.requireCompletionApprover(assignedCase(), UUID.randomUUID(), requester)).doesNotThrowAnyException();
    }

    @Test
    void anonymousCannotHandleCase() {
        assertThatThrownBy(() -> access.requireCaseEditor(unassignedCase(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Adoption applications ───────────────────────

    @Test
    void ownerAndStaffCanDecideAdoption() {
        requesterHasRoleInOrg("OWNER");
        assertThatCode(() -> access.requireAdoptionDecider(orgId, requester)).doesNotThrowAnyException();
        requesterHasRoleInOrg("STAFF");
        assertThatCode(() -> access.requireAdoptionDecider(orgId, requester)).doesNotThrowAnyException();
    }

    @Test
    void vetAndOutsiderCannotDecideAdoption() {
        requesterHasRoleInOrg("VET");
        assertThatThrownBy(() -> access.requireAdoptionDecider(orgId, requester))
                .isInstanceOf(ForbiddenException.class);
        when(orgs.getMemberRole(orgId, requester)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireAdoptionDecider(orgId, requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void applicantCanCancelAndViewButStrangerCannot() {
        UUID applicant = UUID.randomUUID();
        when(orgs.getMemberRole(orgId, requester)).thenReturn(Optional.empty());

        assertThatCode(() -> access.requireAdoptionCanceler(applicant, orgId, applicant)).doesNotThrowAnyException();
        assertThatCode(() -> access.requireAdoptionViewer(applicant, orgId, applicant)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireAdoptionCanceler(applicant, orgId, requester))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> access.requireAdoptionViewer(applicant, orgId, requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void anyOrgMemberCanViewApplicationOfTheirOrg() {
        requesterHasRoleInOrg("VET");
        assertThatCode(() -> access.requireAdoptionViewer(UUID.randomUUID(), orgId, requester))
                .doesNotThrowAnyException();
    }

    // ── Pets ────────────────────────────────────────

    @Test
    void userOwnerManagesPetButStrangerDoesNot() {
        petOwnedBy("USER", reporter);
        assertThatCode(() -> access.requirePetManager(petId, reporter)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requirePetManager(petId, requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void orgStaffAndVetManageOrgPetButMemberRoleDoesNot() {
        petOwnedBy("ORGANIZATION", orgId);
        requesterHasRoleInOrg("VET");
        assertThatCode(() -> access.requirePetManager(petId, requester)).doesNotThrowAnyException();
        requesterHasRoleInOrg("MEMBER");
        assertThatThrownBy(() -> access.requirePetManager(petId, requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void caretakerManagesOrgPet() {
        when(owners.findByPetId(petId)).thenReturn(Optional.of(PetCurrentOwner.builder()
                .petId(petId).ownerType("ORGANIZATION").ownerId(orgId).caretakerUserId(requester).build()));
        when(orgs.getMemberRole(orgId, requester)).thenReturn(Optional.empty());
        assertThatCode(() -> access.requirePetManager(petId, requester)).doesNotThrowAnyException();
    }

    @Test
    void adminManagesAnyPet() {
        requesterIsAdmin();
        assertThatCode(() -> access.requirePetManager(petId, requester)).doesNotThrowAnyException();
    }

    @Test
    void orgPetRecordsArePublic() {
        petOwnedBy("ORGANIZATION", orgId);
        assertThatCode(() -> access.requirePetRecordsReader(petId, null)).doesNotThrowAnyException();
    }

    @Test
    void userPetRecordsArePrivate() {
        petOwnedBy("USER", reporter);
        assertThatCode(() -> access.requirePetRecordsReader(petId, reporter)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requirePetRecordsReader(petId, requester))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> access.requirePetRecordsReader(petId, null))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Generic ─────────────────────────────────────

    @Test
    void selfOrAdminRule() {
        assertThatCode(() -> access.requireSelfOrAdmin(requester, requester)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireSelfOrAdmin(reporter, requester))
                .isInstanceOf(ForbiddenException.class);
        requesterIsAdmin();
        assertThatCode(() -> access.requireSelfOrAdmin(reporter, requester)).doesNotThrowAnyException();
    }
}
