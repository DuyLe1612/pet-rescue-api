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
    private String species;
    @Schema(description = "Priority level", example = "HIGH")
    private RescuePriority priority;
    @Schema(example = "IN_PROGRESS", allowableValues = {"REPORTED", "IN_PROGRESS", "RESCUED", "CLOSED"})
    private String status;
    private String reporterUsername;
    private LocalDateTime reportedAt;
    private String firstImageUrl;
    private String locationText;
    @Schema(description = "Only in nearby results")
    private Double distanceMeters;
}
