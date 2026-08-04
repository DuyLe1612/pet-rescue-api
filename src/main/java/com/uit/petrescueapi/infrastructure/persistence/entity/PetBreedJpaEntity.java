package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "pet_breeds", indexes = {
        @Index(name = "idx_pet_breeds_species", columnList = "species_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetBreedJpaEntity extends BaseJpaEntity {
    @Id
    @Column(name = "breed_id", updatable = false, nullable = false)
    private UUID breedId;

    @Column(name = "species_id", nullable = false)
    private UUID speciesId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
