package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCompletionRequestDto;
import com.uit.petrescueapi.application.dto.rescue.UpdateRescueCaseStatusRequestDto;
import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.rescue.ClaimRescueCaseRequestDto;
import com.uit.petrescueapi.application.dto.rescue.ReleaseRescueClaimRequestDto;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;

import java.util.UUID;

public interface RescueCaseCommandPort {
    RescueCase report(CreateRescueCaseRequestDto cmd, UUID reporterId);
    RescueCase update(UUID caseId, CreateRescueCaseRequestDto cmd, UUID requesterId);
    RescueCase changeStatus(UUID caseId, UpdateRescueCaseStatusRequestDto cmd, UUID requesterId);
    RescueCaseCompletion complete(UUID caseId, CreateRescueCompletionRequestDto cmd, UUID userId);
    RescueCaseCompletion approveCompletion(UUID completionId, UUID userId);
    RescueCaseCompletion confirmCompletion(UUID completionId, UUID reporterId);
    RescueCaseCompletion disputeCompletion(UUID completionId, String reason, UUID reporterId);
    RescueCaseCompletion rejectCompletion(UUID completionId, String reason, UUID userId);

    RescueCaseClaim claim(UUID caseId, ClaimRescueCaseRequestDto request, UUID userId);
    RescueCaseClaim extendClaim(UUID caseId, UUID userId);
    RescueCaseClaim releaseClaim(UUID caseId, ReleaseRescueClaimRequestDto request, UUID userId);

    /** The organization holding a rescued case creates the pet record, in FOSTERING. */
    Pet intake(UUID caseId, CreatePetRequestDto request, UUID userId);

    /** The rescuer whose confirmed report says they keep the animal adds it to their own pets. */
    Pet keepPet(UUID caseId, CreatePetRequestDto request, UUID userId);
}

