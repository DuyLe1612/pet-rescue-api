package com.uit.petrescueapi.application.dto.pet;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new Pet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePetRequestDto {

    @NotBlank(message = "Pet name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Species is required")
    @Size(max = 50)
    private String species;

    @Size(max = 100)
    private String breed;

    @Min(0) @Max(600)
    private Integer age;

    private Gender gender;

    @Size(max = 50)
    private String color;

    @DecimalMin("0.01") @DecimalMax("500.00")
    private BigDecimal weight;

    @Size(max = 2000)
    private String description;

    private boolean vaccinated;

    private boolean neutered;

    private HealthStatus healthStatus;

    @PastOrPresent
    private LocalDate rescueDate;

    @Size(max = 255)
    private String rescueLocation;

    @Size(max = 10)
    private List<String> imageUrls;

    @Size(max = 10)
    private List<UUID> mediaIds;

    private UUID shelterId;

    private UUID rescueCaseId;
}
