package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCompletionRequestDto;
import com.uit.petrescueapi.application.dto.rescue.UpdateRescueCaseStatusRequestDto;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.*;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.RescueCaseMediaRepository;
import com.uit.petrescueapi.domain.repository.RescueCompletionMediaRepository;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MediaQueryPort mediaQueryPort;
    private final MediaCommandPort mediaCommandPort;
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
        return domainService.changeStatus(caseId, newStatus);
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
                        .build();

        RescueCaseCompletion created = domainService.reportCompletion(completion);

        attachCompletionMedia(completionId,cmd.getVerificationImageIds()
        );

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

            if(Objects.equals(media.getStatus(), "TEMP")){
            mediaCommandPort.confirmUpload(mediaId,"rescue-cases/" + caseId
            );
            }

            rescueCaseMediaRepository.save(RescueMedia.builder()
                            .caseId(caseId)
                            .mediaId(mediaId)
                            .build());
        }
    }
    private void attachCompletionMedia(UUID completionId,List<UUID> mediaIds) {

        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

        for (UUID mediaId : mediaIds) {

            var media = mediaQueryPort.findById(mediaId);

            if (media == null) {
                throw new BusinessException("Media not found: " + mediaId);
            }

            mediaCommandPort.confirmUpload(mediaId,"rescue-completions/" + completionId);

            rescueCompletionMediaRepository.save( RescueCompletionMedia.builder()
                            .completionId(completionId)
                            .mediaId(mediaId)
                            .build()
            );
        }
    }

    public RescueCaseCompletion approveCompletion(UUID completionId, UUID userId) {
        log.debug("Command: approve rescue case by user {}", userId);
        RescueCaseCompletion completion = domainService.findCompletionById(completionId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCaseCompletion", "completionId", completionId));
        accessControl.requireCompletionApprover(domainService.findById(completion.getCaseId()), userId);
        return domainService.approveCompletion(completionId,userId);
    }

    private RescueCase buildFromDto(CreateRescueCaseRequestDto cmd) {
        return RescueCase.builder()
                .petId(cmd.getPetId())
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
