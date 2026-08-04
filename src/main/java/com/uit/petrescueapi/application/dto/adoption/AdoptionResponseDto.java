package com.uit.petrescueapi.application.dto.adoption;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for adoption application.
 * Optimized: excludes unused fields (petId, applicantId, applicantUsername, 
 * organizationId, organizationName, note, decidedAt, decidedBy, decidedByUsername, 
 * rejectReason, readyAt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Adoption application")
public class AdoptionResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    @Schema(example = "A-0001")
    private String adoptionCode;

    @Schema(description = "Pet name (denormalized for list views)", example = "Buddy")
    private String petName;

    @Schema(example = "https://cdn.example.com/pets/buddy-primary.jpg")
    private String petPrimaryImageUrl;

    @Schema(example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "CANCELLED","COMPLETE"})
    private String status;

    @Schema(example = "I have experience with dogs and a large backyard.")
    private String experience;

    @Schema(example = "House, salary or pet house")
    private String liveCondition;

    private LocalDateTime createdAt;
}
