package com.uit.petrescueapi.application.dto.pet;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an existing Pet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePetRequestDto {

    @Size(max = 100)
    private String name;

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

    private PetStatus status;
    private HealthStatus healthStatus;
    private boolean vaccinated;
    private boolean neutered;

    @Size(max = 10)
    private List<String> imageUrls;

    @Size(max = 10)
    private List<UUID> mediaIds;
}
