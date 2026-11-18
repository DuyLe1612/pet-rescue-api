package com.uit.petrescueapi.domain.valueobject;

import java.util.UUID;

/** A lost case and a found or rescue case close enough in place, time and species to compare. */
public record CaseMatchCandidate(
        UUID lostCaseId,
        UUID otherCaseId,
        UUID lostReporterId,
        double distanceMeters,
        String lostSpecies,
        String otherSpecies,
        String lostColor,
        String lostMarks,
        String lostDescription,
        String otherColor,
        String otherDescription,
        String otherMarks,
        Double hoursAfterLost) {
}
