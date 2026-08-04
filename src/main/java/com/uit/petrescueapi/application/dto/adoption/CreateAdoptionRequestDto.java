package com.uit.petrescueapi.application.dto.adoption;

import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for submitting an adoption application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdoptionRequestDto {

    @NotNull
    @JsonAlias("pet_id")
    private UUID petId;

    private UUID organizationId;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String experience;

    @NotNull
    @JsonAlias("housing_condition")
    private HousingCondition housingCondition;

    @NotBlank
    @Size(max = 2000)
    private String liveCondition;

    @NotNull
    @JsonAlias({"e_signature_id", "eSignatureId"})
    private UUID signatureMediaId;
}
