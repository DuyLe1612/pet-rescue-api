package com.uit.petrescueapi.infrastructure.persistence.entity;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code pets} table.
 * Lives exclusively in the infrastructure layer — the domain never sees this class.
 */
@Entity
@Table(name = "pets", indexes = {
        @Index(name = "idx_pet_species", columnList = "species_id"),
        @Index(name = "idx_pet_status", columnList = "status"),
        @Index(name = "idx_pet_shelter", columnList = "shelter_id")
})
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "pet_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "pet_code", nullable = false, updatable = false)
    private String petCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "species_id", nullable = false)
    private UUID speciesId;

    @Column(name = "breed_id")
    private UUID breedId;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 30)
    private HealthStatus healthStatus;

    @Column(name = "is_vaccinated", nullable = false)
    private boolean vaccinated;

    @Column(name = "is_neutered", nullable = false)
    private boolean neutered;

    @Column(name = "shelter_id")
    private UUID shelterId;

    @Column(name = "rescue_case_id")
    private UUID rescueCaseId;
}
