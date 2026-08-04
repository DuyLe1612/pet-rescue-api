package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "pet_species")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetSpeciesJpaEntity extends BaseJpaEntity {
    @Id
    @Column(name = "species_id", updatable = false, nullable = false)
    private UUID speciesId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
}
