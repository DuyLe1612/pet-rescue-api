package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.rescue.CaseMatchResponseDto;

import java.util.List;
import java.util.UUID;

public interface CaseMatchQueryDataPort {

    /** Suggestions involving a case, from its side. Dismissed ones are left out. */
    List<CaseMatchResponseDto> findMatches(UUID caseId, boolean caseIsLost);
}
