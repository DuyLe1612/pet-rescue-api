package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the existing {@code pet_ownerships} table.
 * Composite primary key: (pet_id, from_time).
 */
@Entity
@Table(name = "pet_ownerships")
@IdClass(PetOwnershipId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetOwnershipJpaEntity {

    @Id
    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Id
    @Column(name = "from_time", nullable = false)
    private LocalDateTime fromTime;

    @Column(name = "owner_type", length = 20)
    private String ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "to_time")
    private LocalDateTime toTime;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "acquisition_type", length = 20)
    private com.uit.petrescueapi.domain.valueobject.AcquisitionType acquisitionType;

    @Column(name = "adoption_application_id")
    private UUID adoptionApplicationId;
}
