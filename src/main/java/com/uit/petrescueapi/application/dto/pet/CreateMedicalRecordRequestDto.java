package com.uit.petrescueapi.application.dto.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating a pet medical record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicalRecordRequestDto {

    @NotBlank
    @Size(max = 2000)
    private String description;
    @NotBlank
    @Size(max = 255)
    private String vaccine;
    @NotBlank
    @Size(max = 1000)
    private String diagnosis;
    @NotBlank
    @Size(max = 50)
    private String recordDate;
}
