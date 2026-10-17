package com.uit.petrescueapi.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * A species together with all of its breeds, so the FE can load every
 * species/breed option in a single request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Species with its breeds")
public class PetTaxonomyResponseDto {
    private UUID speciesId;

    @Schema(description = "English name (canonical value, use for filters)", example = "Cat")
    private String name;

    @Schema(description = "Vietnamese name for display", example = "Mèo")
    private String nameVi;

    private List<PetBreedResponseDto> breeds;
}
