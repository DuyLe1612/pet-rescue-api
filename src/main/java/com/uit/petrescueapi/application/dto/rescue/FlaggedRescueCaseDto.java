package com.uit.petrescueapi.application.dto.rescue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** An open case the community flagged, with the flags counted by reason. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaggedRescueCaseDto {
    private UUID caseId;
    private String caseCode;
    private String status;
    private String species;
    private String locationText;
    private UUID reporterId;
    /** The reporter's display name (full name, or "Thành viên PawHome"). Never the username. */
    private String reporterName;
    private LocalDateTime reportedAt;
    private int flagCount;
    private int falseReportCount;
    private int alreadyHandledCount;
    private int duplicateCount;
    private LocalDateTime lastFlaggedAt;
}
