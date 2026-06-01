package com.uit.petrescueapi.application.dto.adoption;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for adoption application.
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

    private UUID petId;

    @Schema(description = "Pet name (denormalized for list views)", example = "Buddy")
    private String petName;

    @Schema(example = "https://cdn.example.com/pets/buddy-primary.jpg")
    private String petPrimaryImageUrl;

    private UUID applicantId;

    @Schema(example = "johndoe")
    private String applicantUsername;

    private UUID organizationId;
    private String organizationName;

    @Schema(example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "CANCELLED","COMPLETE"})
    private String status;

    @Schema(example = "Please consider my work schedule and home setup.")
    private String note;

    @Schema(example = "I have experience with dogs and a large backyard.")
    private String experience;

    @Schema(example = "House, salary or pet house")
    private String liveCondition;

    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private UUID decidedBy;
    private String decidedByUsername;
    @Schema(example = "this pets isn't available anymore because it find a new home already")
    private String rejectReason;
    @Schema(example = "12024-12-31T10:00:00")
    private LocalDateTime readyAt;
}
