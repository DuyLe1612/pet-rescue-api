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

    /** English name: canonical value used for filtering. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Vietnamese display name. */
    @Column(name = "name_vi", nullable = false, length = 100)
    private String nameVi;
}
