package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.exception.BaseException;
import com.uit.petrescueapi.domain.exception.UnprocessableEntityException;
import com.uit.petrescueapi.domain.repository.AdoptionApplicationRepository;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.repository.ReclaimLogRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A match warning must be acknowledged, never obeyed: the application still goes to the organization. */
class AdoptionMatchRulesTest {

    final AdoptionApplicationRepository applications = mock(AdoptionApplicationRepository.class);
    final PetDomainService pets = mock(PetDomainService.class);
    final PetCurrentOwnerRepository owners = mock(PetCurrentOwnerRepository.class);
    final AdoptionDomainService adoptions = new AdoptionDomainService(applications, pets,
            mock(VisualCodeRepository.class), mock(OrganizationDomainService.class), mock(AccessControlDomainService.class),
            owners, mock(ReputationDomainService.class), mock(ReclaimLogRepository.class), mock(NotificationDomainService.class));

    final UUID petId = UUID.randomUUID();
    final UUID applicant = UUID.randomUUID();
    final UUID organization = UUID.randomUUID();

    @BeforeEach
    void anAvailableShelterPet() {
        when(pets.findById(petId)).thenReturn(Pet.builder().id(petId).status(PetStatus.AVAILABLE).build());
        when(owners.findByPetId(petId)).thenReturn(Optional.of(
                PetCurrentOwner.builder().petId(petId).ownerType("ORGANIZATION").ownerId(organization).build()));
        when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private AdoptionApplication.AdoptionApplicationBuilder<?, ?> application() {
        return AdoptionApplication.builder().petId(petId).applicantId(applicant);
    }

    @Test
    void aWarningThatWasNotAcknowledgedStopsTheSubmission() {
        AdoptionApplication warned = application().matchWarnings("ALLERGY").warningsAcknowledged(false).build();

        assertThatThrownBy(() -> adoptions.submit(warned))
                .isInstanceOf(UnprocessableEntityException.class)
                .extracting(e -> ((BaseException) e).getErrorCode()).isEqualTo("MATCH_WARNING_NOT_ACKNOWLEDGED");
        verify(applications, never()).save(any());
    }

    @Test
    void anAcknowledgedWarningIsSubmittedAndKeepsItsFlags() {
        AdoptionApplication warned = application().matchLevel("NOT_SUITABLE").matchWarnings("ALLERGY,BUILDING_RULE")
                .warningsAcknowledged(true).build();

        AdoptionApplication saved = adoptions.submit(warned);

        assertThat(saved.getStatus()).isEqualTo(AdoptionApplicationStatus.PENDING);
        assertThat(saved.getOrganizationId()).isEqualTo(organization);
        assertThat(saved.getMatchWarnings()).isEqualTo("ALLERGY,BUILDING_RULE");
        assertThat(saved.isWarningsAcknowledged()).isTrue();
    }

    @Test
    void withoutAWarningNoAcknowledgementIsNeeded() {
        assertThat(adoptions.submit(application().matchLevel("HIGH").build()).getStatus())
                .isEqualTo(AdoptionApplicationStatus.PENDING);
        assertThat(adoptions.submit(application().build()).getStatus()).isEqualTo(AdoptionApplicationStatus.PENDING);
    }

    @Test
    void anUnavailablePetStillBlocksEvenWhenWarningsAreAcknowledged() {
        when(pets.findById(petId)).thenReturn(Pet.builder().id(petId).status(PetStatus.ADOPTED).build());

        assertThatThrownBy(() -> adoptions.submit(application().matchWarnings("ALLERGY").warningsAcknowledged(true).build()))
                .extracting(e -> ((BaseException) e).getErrorCode()).isEqualTo("PET_NOT_AVAILABLE");
    }

    @Test
    void theApplicationLimitStillBlocksAndIsReportedBeforeTheWarning() {
        when(applications.countByApplicantIdAndStatus(applicant, AdoptionApplicationStatus.PENDING)).thenReturn(2L);

        assertThatThrownBy(() -> adoptions.submit(application().matchWarnings("ALLERGY").build()))
                .extracting(e -> ((BaseException) e).getErrorCode()).isEqualTo("ADOPTION_SPAM_LIMIT");
    }
}
