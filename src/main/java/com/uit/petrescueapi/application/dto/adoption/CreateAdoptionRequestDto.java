package com.uit.petrescueapi.application.dto.adoption;

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
    private UUID petId;
    @NotNull
    private UUID organizationId;

    @NotBlank
    @Size(max = 2000)
    private String experience;

    @NotBlank
    @Size(max = 2000)
    private String liveCondition;
}
