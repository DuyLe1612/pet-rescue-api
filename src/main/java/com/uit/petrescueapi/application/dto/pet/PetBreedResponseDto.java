package com.uit.petrescueapi.application.dto.pet;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetBreedResponseDto {
    private UUID breedId;
    private UUID speciesId;
    private String name;
}
