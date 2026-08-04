package com.uit.petrescueapi.application.dto.pet;

import com.uit.petrescueapi.application.dto.organization.OrganizationMinimalDto;
import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Lightweight response DTO for pet list / search results.
 * Matches FE GetAllPetsResponse type.
 * Optimized: excludes unused fields (petCode, provinceCode, wardCode).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pet summary for list views")
public class PetSummaryResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID petId;

    @Schema(example = "Buddy")
    private String name;

    @Schema(example = "Dog")
    private UUID speciesId;
    private String species;

    @Schema(example = "Golden Retriever")
    private UUID breedId;
    private String breed;

    @Schema(example = "24", description = "Age in months")
    private Integer age;

    @Schema(example = "2 years")
    private String ageDisplay;

    @Schema(example = "true")
    private boolean vaccinated;

    private Gender gender;
    private PetStatus status;
    private HealthStatus healthStatus;
    private PetOwnerSummaryDto owner;

    private OrganizationMinimalDto organization;

    @Schema(example = "Hồ Chí Minh")
    private String province;

    @Schema(example = "Phường 1")
    private String ward;

    @Schema(description = "Thumbnail image URL (first image)", example = "https://storage.example.com/pets/buddy-01.jpg")
    private String imageUrl;
}
