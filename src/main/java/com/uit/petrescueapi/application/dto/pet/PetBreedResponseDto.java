package com.uit.petrescueapi.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetBreedResponseDto {
    private UUID breedId;
    private UUID speciesId;

    @Schema(description = "English name (canonical value, use for filters)", example = "Golden Retriever")
    private String name;

    @Schema(description = "Vietnamese name for display", example = "Chó Golden Retriever")
    private String nameVi;
}
