package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.rescue.CaseMatchResponseDto;
import com.uit.petrescueapi.application.dto.rescue.ConvertToRescueRequestDto;
import com.uit.petrescueapi.application.dto.rescue.ReportLostPetRequestDto;
import com.uit.petrescueapi.domain.entity.RescueCase;

import java.util.List;
import java.util.UUID;

/** Lost pets, found animals and the suggestions that link them. */
public interface LostAndFoundPort {

    RescueCase reportLost(UUID petId, ReportLostPetRequestDto request, UUID ownerId);

    RescueCase convertToRescue(UUID caseId, ConvertToRescueRequestDto request, UUID requesterId);

    /** For the reporter of the case, its handlers and admins. Best first. */
    List<CaseMatchResponseDto> findMatches(UUID caseId, UUID requesterId);

    RescueCase confirmMatch(UUID lostCaseId, UUID suggestionId, UUID ownerId);

    void rejectMatch(UUID lostCaseId, UUID suggestionId, UUID ownerId);
}
