package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.application.usecase.RescueCaseCommandUseCase;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.*;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Behaviour fixed in Phase 0 that sits in services rather than in the access rules themselves. */
class Phase0WorkflowRulesTest {

    // ── Rescue ──────────────────────────────────────

    final RescueCaseRepository cases = mock(RescueCaseRepository.class);
    final RescueCaseCompletionRepository completions = mock(RescueCaseCompletionRepository.class);
    final RescueCaseDomainService rescueDomain = new RescueCaseDomainService(cases, completions,
            mock(VisualCodeRepository.class), mock(UserRepository.class), mock(ApplicationEventPublisher.class));

    private RescueCaseCommandUseCase rescueUseCase(RescueCaseDomainService domain, AccessControlDomainService access) {
        return new RescueCaseCommandUseCase(domain, access, mock(MediaQueryPort.class), mock(MediaCommandPort.class),
                mock(RescueCaseMediaRepository.class), mock(RescueCompletionMediaRepository.class));
    }

    private UUID completionForCaseIn(RescueCaseStatus status) {
        UUID caseId = UUID.randomUUID();
        UUID completionId = UUID.randomUUID();
        when(cases.findById(caseId)).thenReturn(Optional.of(RescueCase.builder().caseId(caseId).status(status).build()));
        when(cases.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(completions.findById(completionId)).thenReturn(Optional.of(
                RescueCaseCompletion.builder().completionId(completionId).caseId(caseId).build()));
        when(completions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return completionId;
    }

    @Test
    void reportRecordsTheReporter() {
        RescueCaseDomainService domain = mock(RescueCaseDomainService.class);
        when(domain.report(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID reporter = UUID.randomUUID();

        RescueCase created = rescueUseCase(domain, mock(AccessControlDomainService.class))
                .report(CreateRescueCaseRequestDto.builder().build(), reporter);

        assertThat(created.getReportedBy()).isEqualTo(reporter);
    }

    @Test
    void completionCanBeApprovedFromReportedAndInProgress() {
        UUID approver = UUID.randomUUID();
        for (RescueCaseStatus status : new RescueCaseStatus[]{RescueCaseStatus.REPORTED, RescueCaseStatus.IN_PROGRESS}) {
            RescueCaseCompletion approved = rescueDomain.approveCompletion(completionForCaseIn(status), approver);
            assertThat(approved.getVerifiedBy()).isEqualTo(approver);
        }
    }

    @Test
    void completionCannotBeApprovedOnceClosed() {
        UUID completionId = completionForCaseIn(RescueCaseStatus.CLOSED);
        assertThatThrownBy(() -> rescueDomain.approveCompletion(completionId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approvingUnknownCompletionIsNotFoundNotServerError() {
        UUID unknown = UUID.randomUUID();
        when(completions.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> rescueDomain.approveCompletion(unknown, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void useCaseChecksPermissionBeforeApproving() {
        UUID completionId = completionForCaseIn(RescueCaseStatus.REPORTED);
        UUID caller = UUID.randomUUID();
        AccessControlDomainService access = mock(AccessControlDomainService.class);
        doThrow(new ForbiddenException("no")).when(access).requireCompletionApprover(any(), eqUuid(caller));

        assertThatThrownBy(() -> rescueUseCase(rescueDomain, access).approveCompletion(completionId, caller))
                .isInstanceOf(ForbiddenException.class);
        verify(completions, never()).save(any());
    }

    private static UUID eqUuid(UUID value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    // ── Adoption ────────────────────────────────────

    final AdoptionApplicationRepository applications = mock(AdoptionApplicationRepository.class);
    final PetDomainService pets = mock(PetDomainService.class);
    final PetCurrentOwnerRepository owners = mock(PetCurrentOwnerRepository.class);
    final AccessControlDomainService adoptionAccess = mock(AccessControlDomainService.class);
    final AdoptionDomainService adoptionDomain = new AdoptionDomainService(applications, pets,
            mock(VisualCodeRepository.class), mock(OrganizationDomainService.class), adoptionAccess, owners,
            mock(UserReputationRepository.class), mock(ReclaimLogRepository.class), mock(JdbcTemplate.class));

    private UUID pendingApplication(UUID orgId, UUID applicantId) {
        UUID id = UUID.randomUUID();
        when(applications.findById(id)).thenReturn(Optional.of(AdoptionApplication.builder()
                .applicationId(id).organizationId(orgId).applicantId(applicantId)
                .status(AdoptionApplicationStatus.PENDING).build()));
        return id;
    }

    @Test
    void submitIgnoresClientSuppliedOrganization() {
        UUID petId = UUID.randomUUID();
        UUID realOrg = UUID.randomUUID();
        UUID attackerOrg = UUID.randomUUID();
        when(pets.findById(petId)).thenReturn(Pet.builder().id(petId).status(PetStatus.AVAILABLE).build());
        when(owners.findByPetId(petId)).thenReturn(Optional.of(
                PetCurrentOwner.builder().petId(petId).ownerType("ORGANIZATION").ownerId(realOrg).build()));
        when(applications.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdoptionApplication saved = adoptionDomain.submit(AdoptionApplication.builder()
                .petId(petId).organizationId(attackerOrg).applicantId(UUID.randomUUID()).build());

        assertThat(saved.getOrganizationId()).isEqualTo(realOrg);
    }

    @Test
    void approveAndRejectCheckPermissionBeforeChangingState() {
        UUID orgId = UUID.randomUUID();
        UUID caller = UUID.randomUUID();
        UUID id = pendingApplication(orgId, UUID.randomUUID());
        doThrow(new ForbiddenException("no")).when(adoptionAccess).requireAdoptionDecider(orgId, caller);

        assertThatThrownBy(() -> adoptionDomain.approve(id, caller, LocalDateTime.now()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> adoptionDomain.reject(id, caller, "reason"))
                .isInstanceOf(ForbiddenException.class);
        verify(applications, never()).save(any());
    }

    @Test
    void cancelChecksPermissionBeforeChangingState() {
        UUID orgId = UUID.randomUUID();
        UUID applicant = UUID.randomUUID();
        UUID caller = UUID.randomUUID();
        UUID id = pendingApplication(orgId, applicant);
        doThrow(new ForbiddenException("no")).when(adoptionAccess).requireAdoptionCanceler(applicant, orgId, caller);

        assertThatThrownBy(() -> adoptionDomain.cancel(id, caller)).isInstanceOf(ForbiddenException.class);
        verify(applications, never()).save(any());
    }
}
