package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseUpdateRequestDto;
import com.uit.petrescueapi.application.dto.rescue.FlagRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseFlagSummaryDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseUpdateResponseDto;

import java.util.UUID;

/** Progress notes and community flags on a rescue case. */
public interface RescueCaseCommunityPort {

    RescueCaseUpdateResponseDto postUpdate(UUID caseId, CreateRescueCaseUpdateRequestDto request, UUID authorId);

    RescueCaseFlagSummaryDto flag(UUID caseId, FlagRescueCaseRequestDto request, UUID userId);

    RescueCaseFlagSummaryDto unflag(UUID caseId, UUID userId);
}
