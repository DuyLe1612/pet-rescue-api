package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.adoption.CreateAdoptionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.DecisionRequestDto;
import com.uit.petrescueapi.application.port.command.AdoptionCommandPort;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.service.AdoptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command (write) use-case for Adoption operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link AdoptionDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionCommandUseCase implements AdoptionCommandPort {

    private final AdoptionDomainService domainService;

    @Override
    public AdoptionApplication submit(CreateAdoptionRequestDto cmd, UUID applicantId) {
        log.debug("Command: submit adoption application for pet {} by user {}", cmd.getPetId(), applicantId);
        AdoptionApplication application = AdoptionApplication.builder()
                .petId(cmd.getPetId())
                .organizationId(cmd.getOrganizationId())
                .applicantId(applicantId)
                .experience(cmd.getExperience())
                .liveCondition(cmd.getLiveCondition())
                .build();
        return domainService.submit(application);
    }

    @Override
    public AdoptionApplication approve(UUID applicationId, DecisionRequestDto decision, UUID decidedBy) {
        log.debug("Command: approve adoption application {}", applicationId);
        return domainService.approve(applicationId, decidedBy, decision.getReadyAt());
    }

    @Override
    public AdoptionApplication reject(UUID applicationId, DecisionRequestDto decision, UUID decidedBy) {
        log.debug("Command: reject adoption application {}", applicationId);
        return domainService.reject(applicationId, decidedBy, decision.getRejectReason());
    }

    @Override
    public AdoptionApplication cancel(UUID applicationId) {
        log.debug("Command: cancel adoption application {}", applicationId);
        return domainService.cancel(applicationId);
    }

    @Override
    public AdoptionApplication complete(UUID applicationId, UUID completedBy) {
        log.debug("Command: complete adoption application {}", applicationId);
        return domainService.complete(applicationId, completedBy);
    }
}
