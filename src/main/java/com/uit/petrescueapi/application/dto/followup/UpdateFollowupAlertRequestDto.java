package com.uit.petrescueapi.application.dto.followup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Take an alert (IN_PROGRESS), add a note, or close it (RESOLVED, which needs a resolution). */
public record UpdateFollowupAlertRequestDto(
        @NotNull @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED") String status,
        @Size(max = 1000) String note,
        @Pattern(regexp = "ADVISED|VISITED|RETURNED|NO_ACTION_NEEDED|UNREACHABLE|OTHER") String resolution) {
}
