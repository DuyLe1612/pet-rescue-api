package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.adoption.CreateAdoptionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.DecisionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.ReclaimPetRequestDto;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.ReclaimLog;

import java.util.UUID;

public interface AdoptionCommandPort {
    AdoptionApplication submit(CreateAdoptionRequestDto cmd, UUID applicantId);

    /**
     * Scores open applications that were filed before both sides could be compared (no adopter
     * profile or no pet traits yet), with the same evaluation as on submit. Returns how many were scored.
     */
    int backfillMatches(int limit);
    AdoptionApplication approve(UUID applicationId, DecisionRequestDto decision, UUID decidedBy);
    AdoptionApplication reject(UUID applicationId, DecisionRequestDto decision, UUID decidedBy);
    AdoptionApplication cancel(UUID applicationId, UUID requesterId);
    
    /**
     * Complete an approved adoption.
     * This transfers pet ownership from the organization to the adopter.
     * Only APPROVED applications can be completed.
     */
    AdoptionApplication complete(UUID applicationId, UUID completedBy);

    ReclaimLog reclaim(UUID applicationId, ReclaimPetRequestDto request, UUID reclaimedBy);
}
