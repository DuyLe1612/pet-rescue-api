package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.media.MediaFileResponseDto;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.event.RescueCaseReportedEvent;
import com.uit.petrescueapi.domain.repository.RescueCaseMediaRepository;
import com.uit.petrescueapi.domain.repository.RescueCompletionMediaRepository;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.PetDomainService;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.service.RescueClaimDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reporting tells the triage a case exists, after its photos, and never waits for it. */
class RescueReportEventTest {

    final RescueCaseDomainService domain = mock(RescueCaseDomainService.class);
    final MediaQueryPort mediaQuery = mock(MediaQueryPort.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final RescueCaseMediaRepository caseMedia = mock(RescueCaseMediaRepository.class);
    final RescueCaseCommandUseCase useCase = new RescueCaseCommandUseCase(domain, mock(AccessControlDomainService.class),
            mock(RescueClaimDomainService.class), mock(PetCommandPort.class), mock(PetDomainService.class), mediaQuery,
            events, caseMedia, mock(RescueCompletionMediaRepository.class));

    @Test
    void aReportWithoutPhotosPublishesTheEvent() {
        when(domain.report(any())).thenAnswer(call -> call.getArgument(0));

        RescueCase created = useCase.report(CreateRescueCaseRequestDto.builder().species("Dog").build(), UUID.randomUUID());

        verify(events).publishEvent(new RescueCaseReportedEvent(created.getCaseId()));
    }

    @Test
    void thePhotosAreAttachedBeforeTheTriageCanLookForThem() {
        UUID mediaId = UUID.randomUUID();
        MediaFileResponseDto media = mock(MediaFileResponseDto.class);
        when(media.getStatus()).thenReturn("PERMANENT");
        when(mediaQuery.findById(mediaId)).thenReturn(media);
        when(domain.report(any())).thenAnswer(call -> call.getArgument(0));

        RescueCase created = useCase.report(CreateRescueCaseRequestDto.builder().species("Dog").mediaIds(List.of(mediaId)).build(),
                UUID.randomUUID());

        InOrder order = inOrder(caseMedia, events);
        order.verify(caseMedia).save(any());
        order.verify(events).publishEvent(new RescueCaseReportedEvent(created.getCaseId()));
    }

    @Test
    void aReportThatFailsPublishesNothing() {
        when(domain.report(any())).thenThrow(new IllegalStateException("db down"));

        try {
            useCase.report(CreateRescueCaseRequestDto.builder().species("Dog").build(), UUID.randomUUID());
        } catch (IllegalStateException expected) {
            // the point is what did not happen
        }

        verify(events, never()).publishEvent(any(RescueCaseReportedEvent.class));
    }
}
