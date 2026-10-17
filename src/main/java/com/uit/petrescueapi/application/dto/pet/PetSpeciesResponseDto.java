package com.uit.petrescueapi.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetSpeciesResponseDto {
    private UUID speciesId;

    @Schema(description = "English name (canonical value, use for filters)", example = "Dog")
    private String name;

    @Schema(description = "Vietnamese name for display", example = "Chó")
    private String nameVi;
}
