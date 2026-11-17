package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ultra-lightweight DTO for map markers.
 * Optimized for fast rendering (~100 bytes vs ~2KB full DTO).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rescue case map marker - minimal data for fast map rendering")
public class RescueMapMarkerDto {
    
    @Schema(description = "Rescue case ID")
    private UUID caseId;

    @Schema(description = "Visual rescue case code", example = "R-0001")
    private String caseCode;
    
    @Schema(description = "Latitude coordinate")
    private Double latitude;
    
    @Schema(description = "Longitude coordinate")
    private Double longitude;
    
    @Schema(description = "Priority level", example = "HIGH")
    private RescuePriority priority;
    
    @Schema(description = "Case status", example = "REPORTED")
    private RescueCaseStatus status;
    
    @Schema(description = "Animal species", example = "Dog")
    private String species;
    
    @Schema(description = "The people on the case asked for more help")
    private boolean needsHelp;

    @Schema(description = "When the case was reported")
    private LocalDateTime reportedAt;
}
