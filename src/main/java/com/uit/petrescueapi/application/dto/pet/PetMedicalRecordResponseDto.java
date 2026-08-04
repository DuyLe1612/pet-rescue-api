package com.uit.petrescueapi.application.dto.pet;

import com.uit.petrescueapi.domain.valueobject.MedicalRecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for pet medical record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pet medical record")
public class PetMedicalRecordResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID recordId;

    private UUID petId;
    private MedicalRecordType recordType;

    @Schema(example = "Annual checkup — all healthy")
    private String description;

    @Schema(example = "Rabies vaccine (3-year)")
    private String vaccine;

    @Schema(example = "No issues found")
    private String diagnosis;

    private LocalDateTime recordDate;
    private UUID createdBy;
}
