package com.uit.petrescueapi.application.dto.adoption;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight response DTO for adoption application list views.
 * Optimized: excludes applicantUsername (not used by FE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Adoption application summary for list views")
public class AdoptionSummaryResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    @Schema(example = "A-0001")
    private String adoptionCode;

    @Schema(example = "Buddy")
    private String petName;

    @Schema(example = "https://cdn.example.com/pets/buddy-primary.jpg")
    private String petPrimaryImageUrl;

    @Schema(example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"})
    private String status;

    @Schema(example = "i used to have a cat 2 years ago")
    private String experience;

    @Schema(example = "Apartment at Ha Noi, avg salary 2000$")
    private String liveCondition;

    private LocalDateTime createdAt;
}
