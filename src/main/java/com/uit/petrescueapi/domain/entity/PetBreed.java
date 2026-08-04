package com.uit.petrescueapi.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetBreed extends BaseEntity {
    private UUID breedId;
    private UUID speciesId;
    private String name;
}
