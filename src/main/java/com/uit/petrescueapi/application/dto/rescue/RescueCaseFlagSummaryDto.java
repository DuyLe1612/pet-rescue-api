package com.uit.petrescueapi.application.dto.rescue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** How many people flagged a case, after the caller flagged or withdrew. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescueCaseFlagSummaryDto {
    private UUID caseId;
    private int flagCount;
    private boolean viewerHasFlagged;
}
