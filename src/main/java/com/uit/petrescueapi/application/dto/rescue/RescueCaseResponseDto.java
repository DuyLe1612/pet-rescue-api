package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for rescue case.
 * Optimized: excludes unused fields (petId, petName, reportedBy, organizationId, 
 * organizationName, size, locationText, wardName, provinceName, resolvedAt, createdAt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rescue case")
public class RescueCaseResponseDto {
    private UUID caseId;
    private String caseCode;
    private String reporterUsername;
    private String species;
    private String color;
    @Schema(description = "Priority level", example = "HIGH")
    private RescuePriority priority;
    private String description;
    @Schema(example = "IN_PROGRESS", allowableValues = {"REPORTED", "IN_PROGRESS", "RESCUED", "CLOSED"})
    private String status;
    private Double latitude;
    private Double longitude;
    private LocalDateTime reportedAt;
    private List<String> imageUrls;
    private String contactPhone;
}
