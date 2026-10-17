package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.event.RescueCaseStatusChangedEvent;
import com.uit.petrescueapi.domain.event.UserProfileChangedEvent;
import com.uit.petrescueapi.domain.repository.RescueCaseCompletionRepository;
import com.uit.petrescueapi.domain.repository.RescueCaseRepository;
import com.uit.petrescueapi.domain.repository.RoleRepository;
import com.uit.petrescueapi.domain.repository.UserRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Domain services announce changes to data that MongoDB documents keep snapshots of. */
class SnapshotEventPublishingTest {

    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    @Test
    void profileChangeOfDisplayedFieldsPublishesEvent() {
        UserRepository users = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).username("old").build()));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UserDomainService service = new UserDomainService(users, mock(RoleRepository.class), mock(VisualCodeRepository.class), events);

        service.updateProfile(id, null, "New Name", null, null, null, null, null, null, null, null);

        verify(events).publishEvent(new UserProfileChangedEvent(id));
    }

    @Test
    void profileChangeOfOtherFieldsPublishesNothing() {
        UserRepository users = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).build()));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UserDomainService service = new UserDomainService(users, mock(RoleRepository.class), mock(VisualCodeRepository.class), events);

        service.updateProfile(id, null, null, null, "0909", null, null, null, null, null, null);

        verifyNoInteractions(events);
    }

    @Test
    void rescueStatusChangePublishesEvent() {
        RescueCaseRepository cases = mock(RescueCaseRepository.class);
        UUID caseId = UUID.randomUUID();
        when(cases.findById(caseId)).thenReturn(Optional.of(RescueCase.builder().caseId(caseId).status(RescueCaseStatus.REPORTED).build()));
        when(cases.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RescueCaseDomainService service = new RescueCaseDomainService(cases, mock(RescueCaseCompletionRepository.class),
                mock(VisualCodeRepository.class), mock(UserRepository.class), events);

        service.changeStatus(caseId, RescueCaseStatus.IN_PROGRESS);

        verify(events).publishEvent(new RescueCaseStatusChangedEvent(caseId, "IN_PROGRESS"));
    }
}
