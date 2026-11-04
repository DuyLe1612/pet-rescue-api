package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.rescue.ClaimRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.ReleaseRescueClaimRequestDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCompletionRequestDto;
import com.uit.petrescueapi.application.dto.rescue.UpdateRescueCaseStatusRequestDto;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.*;
import com.uit.petrescueapi.domain.event.MediaPromotionRequestedEvent;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.RescueCaseMediaRepository;
import com.uit.petrescueapi.domain.repository.RescueCompletionMediaRepository;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.PetDomainService;
import com.uit.petrescueapi.domain.service.RescueClaimDomainService;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.domain.valueobject.RescueCompletionStatus;
import com.uit.petrescueapi.domain.valueobject.RescueOutcome;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Command (write) use-case for RescueCase operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link RescueCaseDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RescueCaseCommandUseCase implements RescueCaseCommandPort {

    private final RescueCaseDomainService domainService;
    private final AccessControlDomainService accessControl;
    private final RescueClaimDomainService claimDomainService;
    private final PetCommandPort petCommandPort;
    private final PetDomainService petDomainService;
    private final MediaQueryPort mediaQueryPort;
    private final ApplicationEventPublisher events;
    private final RescueCaseMediaRepository rescueCaseMediaRepository;
    private final RescueCompletionMediaRepository rescueCompletionMediaRepository;

    @Override
    public RescueCase report(CreateRescueCaseRequestDto cmd, UUID reporterId) {

        log.debug("Command: report rescue case by user {}", reporterId);

        UUID caseId = UUID.randomUUID();

        RescueCase rescueCase = buildFromDto(cmd);
        rescueCase.setCaseId(caseId);
        rescueCase.setReportedBy(reporterId);

        RescueCase created = domainService.report(rescueCase);

        attachCaseMedia(caseId, cmd.getMediaIds());

        return created;
    }

    @Override
    public RescueCase update(UUID caseId, CreateRescueCaseRequestDto cmd, UUID requesterId) {

        log.debug( "Command: update rescue case {}",caseId);

        accessControl.requireCaseEditor(domainService.findById(caseId), requesterId);

        RescueCase patch = buildFromDto(cmd);

        return domainService.update(caseId,patch);
    }

    @Override
    public RescueCase changeStatus(UUID caseId, UpdateRescueCaseStatusRequestDto cmd, UUID requesterId) {
        log.debug("Command: change status of rescue case {} to {}", caseId, cmd.getStatus());
        RescueCaseStatus newStatus = RescueCaseStatus.valueOf(cmd.getStatus());
        accessControl.requireCaseStatusChange(domainService.findById(caseId), newStatus, requesterId);
        String reason = cmd.getReason() == null || cmd.getReason().isBlank() ? null : cmd.getReason().trim();
        if (newStatus == RescueCaseStatus.CLOSED && reason == null && !accessControl.isAdmin(requesterId)) {
            throw new BusinessException("A reason is required to close a case", "RESCUE_CLOSE_REASON_REQUIRED");
        }
        return domainService.changeStatus(caseId, newStatus, reason, requesterId);
    }

    @Override
    public RescueCaseCompletion complete(UUID caseId,  CreateRescueCompletionRequestDto cmd,  UUID userId) {

        log.debug("Command: complete rescue case by user {}", userId    );

        UUID completionId = UUID.randomUUID();

        RescueCaseCompletion completion =     RescueCaseCompletion.builder()
                        .completionId(completionId)
                        .caseId(caseId)
                        .rescuedAt(cmd.getRescuedAt())
                        .rescueNote(cmd.getRescueNote())
                        .locationNote(cmd.getLocationNote())
                        .submittedBy(userId)
                        .outcome(cmd.getOutcome())
                        .build();

        RescueCaseCompletion created = domainService.reportCompletion(completion);

        attachCompletionMedia(completionId, cmd.getVerificationImageIds(), userId);

        return created;
    }

    private void attachCaseMedia(UUID caseId,  List<UUID> mediaIds) {

        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

        for (UUID mediaId : mediaIds) {

            var media = mediaQueryPort.findById(mediaId);

            if (media == null) {
                throw new BusinessException("Media not found: " + mediaId);
            }

            if (Objects.equals(media.getStatus(), "TEMP")) {
                events.publishEvent(new MediaPromotionRequestedEvent(mediaId, "rescue-cases/" + caseId));
            }

            rescueCaseMediaRepository.save(RescueMedia.builder()
                            .caseId(caseId)
                            .mediaId(mediaId)
                            .build());
        }
    }
    /** Evidence must be the sender's own upload, so nobody passes off another person's photo. */
    private void attachCompletionMedia(UUID completionId, List<UUID> mediaIds, UUID userId) {

        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

        for (UUID mediaId : mediaIds) {

            var media = mediaQueryPort.findById(mediaId);

            if (media == null) {
                throw new BusinessException("Media not found: " + mediaId);
            }

            accessControl.requireSelfOrAdmin(media.getUploaderId(), userId);

            if (Objects.equals(media.getStatus(), "TEMP")) {
                events.publishEvent(new MediaPromotionRequestedEvent(mediaId, "rescue-completions/" + completionId));
            }

            rescueCompletionMediaRepository.save( RescueCompletionMedia.builder()
                            .completionId(completionId)
                            .mediaId(mediaId)
                            .build()
            );
        }
    }

    public RescueCaseCompletion approveCompletion(UUID completionId, UUID userId) {
        log.debug("Command: approve rescue case by user {}", userId);
        requireCompletionReviewer(completionId, userId);
        return domainService.approveCompletion(completionId,userId);
    }

    @Override
    public RescueCaseCompletion confirmCompletion(UUID completionId, UUID reporterId) {
        return domainService.confirmCompletion(completionId, reporterId);
    }

    @Override
    public RescueCaseCompletion disputeCompletion(UUID completionId, String reason, UUID reporterId) {
        return domainService.disputeCompletion(completionId, reason, reporterId);
    }

    @Override
    public RescueCaseCompletion rejectCompletion(UUID completionId, String reason, UUID userId) {
        requireCompletionReviewer(completionId, userId);
        return domainService.rejectCompletion(completionId, reason, userId);
    }

    /** A dispute already put the reporter against the rescuer, so only an admin settles it. */
    private void requireCompletionReviewer(UUID completionId, UUID userId) {
        RescueCaseCompletion completion = domainService.findCompletionById(completionId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCaseCompletion", "completionId", completionId));
        if (completion.getStatus() == RescueCompletionStatus.DISPUTED) {
            accessControl.requireAdmin(userId);
            return;
        }
        accessControl.requireCompletionApprover(domainService.findById(completion.getCaseId()), completion.getSubmittedBy(), userId);
    }

    @Override
    public RescueCaseClaim claim(UUID caseId, ClaimRescueCaseRequestDto request, UUID userId) {
        return claimDomainService.claim(caseId, userId, request != null && request.isSafetyAcknowledged());
    }

    @Override
    public RescueCaseClaim extendClaim(UUID caseId, UUID userId) {
        return claimDomainService.extend(caseId, userId);
    }

    @Override
    public RescueCaseClaim releaseClaim(UUID caseId, ReleaseRescueClaimRequestDto request, UUID userId) {
        return claimDomainService.release(caseId, userId, request == null ? null : request.getReason());
    }

    @Override
    public Pet intake(UUID caseId, CreatePetRequestDto request, UUID userId) {
        RescueCase rescueCase = domainService.findById(caseId);
        if (rescueCase.getOrganizationId() == null) {
            throw new ConflictException("Claim this case for your organization first", "RESCUE_CASE_NOT_CLAIMED");
        }
        accessControl.requireOrganizationHandler(rescueCase.getOrganizationId(), userId);
        if (rescueCase.getStatus() != RescueCaseStatus.RESCUED) {
            throw new ConflictException("Only a rescued case can be taken in", "RESCUE_CASE_NOT_RESCUED");
        }
        requireNoPet(rescueCase);
        request.setRescueCaseId(caseId);
        Pet pet = petCommandPort.createForShelter(request, rescueCase.getOrganizationId(), userId);
        Pet fostering = petDomainService.changeStatus(pet.getId(), PetStatus.FOSTERING);
        domainService.linkPet(caseId, pet.getId());
        return fostering;
    }

    @Override
    public Pet keepPet(UUID caseId, CreatePetRequestDto request, UUID userId) {
        RescueCase rescueCase = domainService.findById(caseId);
        RescueCaseCompletion completion = domainService.findOpenCompletion(caseId)
                .filter(c -> c.getStatus() == RescueCompletionStatus.APPROVED)
                .filter(c -> c.getOutcome() == RescueOutcome.KEPT_BY_RESCUER)
                .orElseThrow(() -> new ConflictException("No confirmed report says the rescuer keeps this animal", "RESCUE_CASE_NOT_KEPT"));
        if (!userId.equals(completion.getSubmittedBy())) {
            throw new ForbiddenException("Only the rescuer can add this animal to their pets");
        }
        requireNoPet(rescueCase);
        request.setShelterId(null);
        request.setRescueCaseId(caseId);
        Pet pet = petCommandPort.createForUser(request, userId);
        domainService.linkPet(caseId, pet.getId());
        return pet;
    }

    private static void requireNoPet(RescueCase rescueCase) {
        if (rescueCase.getPetId() != null) {
            throw new ConflictException("This case already has a pet record", "RESCUE_CASE_HAS_PET");
        }
    }

    private RescueCase buildFromDto(CreateRescueCaseRequestDto cmd) {
        return RescueCase.builder()
                .species(cmd.getSpecies())
                .color(cmd.getColor())
                .size(cmd.getSize())
                .priority(cmd.getPriority())
                .description(cmd.getDescription())
                .latitude(cmd.getLatitude())
                .longitude(cmd.getLongitude())
                .locationText(cmd.getLocationText())
                .provinceCode(cmd.getProvinceCode())
                .provinceName(cmd.getProvinceName())
                .wardCode(cmd.getWardCode())
                .wardName(cmd.getWardName())
                .contactPhone(cmd.getContactPhone())
                .build();
    }
}
