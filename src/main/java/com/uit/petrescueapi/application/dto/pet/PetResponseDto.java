package com.uit.petrescueapi.application.dto.pet;

import com.uit.petrescueapi.application.dto.organization.OrganizationMinimalDto;
import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Pet detail.
 * Matches FE Pet interface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponseDto {

    private UUID petId;
    private String petCode;
    private String name;
    private String species;
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
    private Integer provinceCode;
    private String ward;
    private Integer wardCode;

    // Detail fields
    private String color;
    private BigDecimal weight;
    private String description;
    private boolean neutered;
    private LocalDate rescueDate;
    private String rescueLocation;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private UUID shelterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
