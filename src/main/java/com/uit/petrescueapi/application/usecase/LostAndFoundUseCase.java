package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.rescue.CaseMatchResponseDto;
import com.uit.petrescueapi.application.dto.rescue.ConvertToRescueRequestDto;
import com.uit.petrescueapi.application.dto.rescue.ReportLostPetRequestDto;
import com.uit.petrescueapi.application.port.command.LostAndFoundPort;
import com.uit.petrescueapi.application.port.out.CaseMatchQueryDataPort;
import com.uit.petrescueapi.application.port.query.PetQueryPort;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.event.RescueCaseReportedEvent;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.LostAndFoundDomainService;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.valueobject.RescueCaseType;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LostAndFoundUseCase implements LostAndFoundPort {

    private final LostAndFoundDomainService domainService;
    private final RescueCaseDomainService caseDomainService;
    private final AccessControlDomainService accessControl;
    private final PetQueryPort petQueryPort;
    private final CaseMatchQueryDataPort matchQuery;
    private final ApplicationEventPublisher events;

    @Override
    public RescueCase reportLost(UUID petId, ReportLostPetRequestDto request, UUID ownerId) {
        // checked before reading the profile, so a stranger learns nothing about a private pet
        accessControl.requirePetManager(petId, ownerId);
        LocalDateTime lastSeen = request.getLastSeenAt() == null ? LocalDateTime.now() : request.getLastSeenAt();
        if (lastSeen.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException("lastSeenAt cannot be in the future", "LAST_SEEN_IN_FUTURE");
        }
        var pet = petQueryPort.findById(petId);
        RescueCase draft = RescueCase.builder()
                .caseId(UUID.randomUUID())
                .species(pet.getSpecies())
                .color(pet.getColor())
                .priority(RescuePriority.NORMAL)
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationText(request.getLocationText())
                .provinceCode(request.getProvinceCode())
                .provinceName(request.getProvinceName())
                .wardCode(request.getWardCode())
                .wardName(request.getWardName())
                .contactPhone(request.getContactPhone())
                .lastSeenAt(lastSeen)
                .distinctiveMarks(request.getDistinctiveMarks())
                .build();
        RescueCase created = domainService.reportLost(draft, petId, ownerId);
        events.publishEvent(new RescueCaseReportedEvent(created.getCaseId()));
        return created;
    }

    @Override
    public RescueCase convertToRescue(UUID caseId, ConvertToRescueRequestDto request, UUID requesterId) {
        accessControl.requireCaseEditor(caseDomainService.findById(caseId), requesterId);
        RescueCase converted = caseDomainService.convertToRescue(caseId, request == null ? null : request.getPriority());
        // a rescue now: the AI labels it and lost pets nearby are compared again
        events.publishEvent(new RescueCaseReportedEvent(caseId));
        return converted;
    }

    @Override
    public List<CaseMatchResponseDto> findMatches(UUID caseId, UUID requesterId) {
        RescueCase rescueCase = caseDomainService.findById(caseId);
        accessControl.requireCaseEditor(rescueCase, requesterId);
        return matchQuery.findMatches(caseId, rescueCase.typeOrRescue() == RescueCaseType.LOST);
    }

    @Override
    public RescueCase confirmMatch(UUID lostCaseId, UUID suggestionId, UUID ownerId) {
        return domainService.confirm(lostCaseId, suggestionId, ownerId);
    }

    @Override
    public void rejectMatch(UUID lostCaseId, UUID suggestionId, UUID ownerId) {
        domainService.reject(lostCaseId, suggestionId, ownerId);
    }
}
