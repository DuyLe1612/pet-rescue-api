package com.uit.petrescueapi.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Display DTO for a pet ownership history entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pet ownership history display entry")
public class PetOwnershipHistoryDisplayDto {

    @Schema(example = "USER", allowableValues = {"USER", "ORGANIZATION"})
    private String ownerType;

    @Schema(example = "John Doe / Happy Paws Shelter")
    private String ownerName;

    @Schema(example = "2026-05-27T14:30:00")
    private LocalDateTime timestamp;
}