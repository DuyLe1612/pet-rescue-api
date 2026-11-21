package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for rescue case list views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rescue case summary")
public class RescueCaseSummaryResponseDto {
    private UUID caseId;
    private String caseCode;
    @Schema(example = "RESCUE", allowableValues = {"RESCUE", "LOST", "FOUND"})
    private String caseType;
    @Schema(description = "Set when latitude and longitude are moved by up to this many metres")
    private Integer locationBlurMeters;
    private String species;
    @Schema(description = "Priority level", example = "HIGH")
    private RescuePriority priority;
    @Schema(example = "IN_PROGRESS", allowableValues = {"REPORTED", "IN_PROGRESS", "RESCUED", "CLOSED"})
    private String status;
    /** The reporter's display name (full name, or "Thành viên PawHome"). Never the username. */
    private String reporterName;
    private LocalDateTime reportedAt;
    private String firstImageUrl;
    private String locationText;
    @Schema(description = "Only in nearby results")
    private Double distanceMeters;
    private Double latitude;
    private Double longitude;
    private String wardName;
    private String provinceName;
    private boolean needsHelp;
}
