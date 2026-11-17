package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.media.MediaFileResponseDto;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseUpdateRequestDto;
import com.uit.petrescueapi.application.dto.rescue.FlagRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.FlaggedRescueCaseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseFlagSummaryDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseUpdateResponseDto;
import com.uit.petrescueapi.application.port.command.RescueCaseCommunityPort;
import com.uit.petrescueapi.application.port.out.RescueCaseCommunityQueryDataPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.application.port.query.RescueCaseCommunityQueryPort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.RescueCaseCommunityDomainService;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Thin orchestration: media checks here, rules and writes in {@link RescueCaseCommunityDomainService}. */
@Service
@RequiredArgsConstructor
public class RescueCaseCommunityUseCase implements RescueCaseCommunityPort, RescueCaseCommunityQueryPort {

    private final RescueCaseCommunityDomainService domainService;
    private final RescueCaseDomainService caseDomainService;
    private final AccessControlDomainService accessControl;
    private final MediaQueryPort mediaQueryPort;
    private final RescueCaseCommunityQueryDataPort queryDataPort;

    @Override
    public RescueCaseUpdateResponseDto postUpdate(UUID caseId, CreateRescueCaseUpdateRequestDto request, UUID authorId) {
        List<UUID> mediaIds = request.getMediaIds() == null ? List.of()
                : request.getMediaIds().stream().filter(Objects::nonNull).distinct().toList();
        Set<UUID> temporary = new HashSet<>();
        for (UUID mediaId : mediaIds) {
            MediaFileResponseDto media = mediaQueryPort.findById(mediaId);
            if (media == null) {
                throw new BusinessException("Media not found: " + mediaId);
            }
            // a progress photo is evidence, so it must be the author's own
            accessControl.requireSelfOrAdmin(media.getUploaderId(), authorId);
            if ("TEMP".equals(media.getStatus())) {
                temporary.add(mediaId);
            }
        }
        UUID updateId = domainService.postUpdate(caseId, authorId, request.getNote(), mediaIds, temporary);
        return queryDataPort.findUpdate(updateId).orElseThrow();
    }

    @Override
    public RescueCaseFlagSummaryDto flag(UUID caseId, FlagRescueCaseRequestDto request, UUID userId) {
        int count = domainService.flag(caseId, userId, request.getReason(), request.getNote());
        return new RescueCaseFlagSummaryDto(caseId, count, true);
    }

    @Override
    public RescueCaseFlagSummaryDto unflag(UUID caseId, UUID userId) {
        return new RescueCaseFlagSummaryDto(caseId, domainService.unflag(caseId, userId), false);
    }

    @Override
    public Page<RescueCaseUpdateResponseDto> findUpdates(UUID caseId, Pageable pageable) {
        caseDomainService.findById(caseId);
        return queryDataPort.findUpdates(caseId, pageable);
    }

    @Override
    public Page<FlaggedRescueCaseDto> findFlaggedCases(Pageable pageable) {
        return queryDataPort.findFlaggedCases(pageable);
    }
}
