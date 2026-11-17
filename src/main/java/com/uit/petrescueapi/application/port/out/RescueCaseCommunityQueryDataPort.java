package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.rescue.FlaggedRescueCaseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RescueCaseCommunityQueryDataPort {

    Page<RescueCaseUpdateResponseDto> findUpdates(UUID caseId, Pageable pageable);

    Optional<RescueCaseUpdateResponseDto> findUpdate(UUID updateId);

    Page<FlaggedRescueCaseDto> findFlaggedCases(Pageable pageable);

    boolean hasFlagged(UUID caseId, UUID userId);
}
