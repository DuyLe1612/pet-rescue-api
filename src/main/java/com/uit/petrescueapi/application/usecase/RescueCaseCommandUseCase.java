package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.UpdateRescueCaseStatusRequestDto;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command (write) use-case for RescueCase operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link RescueCaseDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RescueCaseCommandUseCase implements RescueCaseCommandPort {

    private final RescueCaseDomainService domainService;
    private final MediaQueryPort mediaQueryPort;

    @Override
    public RescueCase report(CreateRescueCaseRequestDto cmd, UUID reporterId) {
        log.debug("Command: report rescue case by user {}", reporterId);
        RescueCase rescueCase = buildFromDto(cmd);
        rescueCase.setReportedBy(reporterId);
        return domainService.report(rescueCase);
    }

    @Override
    public RescueCase update(UUID caseId, CreateRescueCaseRequestDto cmd) {
        log.debug("Command: update rescue case {}", caseId);
        RescueCase patch = buildFromDto(cmd);
        return domainService.update(caseId, patch);
    }

    @Override
    public RescueCase changeStatus(UUID caseId, UpdateRescueCaseStatusRequestDto cmd) {
        log.debug("Command: change status of rescue case {} to {}", caseId, cmd.getStatus());
        RescueCaseStatus newStatus = RescueCaseStatus.valueOf(cmd.getStatus());
        return domainService.changeStatus(caseId, newStatus);
    }

    private RescueCase buildFromDto(CreateRescueCaseRequestDto cmd) {
        return RescueCase.builder()
                .petId(cmd.getPetId())
                .organizationId(cmd.getOrganizationId())
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
                .imagePublicIds(resolvePublicIds(cmd.getMediaIds(), cmd.getImageUrls()))
                .contactPhone(cmd.getContactPhone())
                .build();
    }

    private java.util.List<String> resolvePublicIds(java.util.List<java.util.UUID> mediaIds, java.util.List<String> imageUrls) {
        if (mediaIds != null && !mediaIds.isEmpty()) {
            return mediaIds.stream()
                    .map(mediaQueryPort::findById)
                    .map(media -> media.getPublicId())
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return imageUrls;
    }
}
