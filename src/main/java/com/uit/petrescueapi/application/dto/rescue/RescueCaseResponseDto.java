package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for rescue case.
 * Contains names (wardName, provinceName) - codes are not exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rescue case")
public class RescueCaseResponseDto {
    private UUID caseId;
    private String caseCode;
    private UUID petId;
    private String petName;
    private UUID reportedBy;
    private String reporterUsername;
    private UUID organizationId;
    private String organizationName;
    private String species;
    private String color;
    private String size;
    @Schema(description = "Priority level", example = "HIGH")
    private RescuePriority priority;
    private String description;
    @Schema(example = "IN_PROGRESS", allowableValues = {"REPORTED", "IN_PROGRESS", "RESCUED", "CLOSED"})
    private String status;
    private Double latitude;
    private Double longitude;
    private String locationText;
    private String wardName;
    private String provinceName;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private String contactPhone;
}
