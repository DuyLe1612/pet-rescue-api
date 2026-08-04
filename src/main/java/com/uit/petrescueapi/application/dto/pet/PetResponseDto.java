package com.uit.petrescueapi.application.dto.pet;

import com.uit.petrescueapi.application.dto.organization.OrganizationMinimalDto;
import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Pet detail.
 * Matches FE Pet interface.
 * Optimized: excludes unused fields (petCode, shelterId, createdAt, updatedAt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponseDto {

    private UUID petId;
    private String name;
    private UUID speciesId;
    private String species;
    private UUID breedId;
    private String breed;
    private Integer age;
    private String ageDisplay;
    private boolean vaccinated;
    private Gender gender;
    private PetStatus status;
    private HealthStatus healthStatus;
    private PetOwnerSummaryDto owner;

    private OrganizationMinimalDto organization;

    private String province;
    private String ward;

    // Detail fields
    private String color;
    private BigDecimal weight;
    private String description;
    private boolean neutered;
    private LocalDate rescueDate;
    private String rescueLocation;
    private UUID rescueCaseId;
    private String primaryImageUrl;
    private List<String> imageUrls;
}
