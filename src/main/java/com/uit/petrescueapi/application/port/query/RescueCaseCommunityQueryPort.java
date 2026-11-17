package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.rescue.FlaggedRescueCaseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RescueCaseCommunityQueryPort {

    /** Newest first. Public, like the case itself. */
    Page<RescueCaseUpdateResponseDto> findUpdates(UUID caseId, Pageable pageable);

    /** Open cases with at least one flag, most flagged first. For admins. */
    Page<FlaggedRescueCaseDto> findFlaggedCases(Pageable pageable);
}
