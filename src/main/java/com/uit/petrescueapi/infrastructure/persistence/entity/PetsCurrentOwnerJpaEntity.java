package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * JPA entity mapped to the {@code pets_current_owner} table.
 * Simple tracking table — does not extend BaseJpaEntity.
 */
@Entity
@Table(name = "pets_current_owner", indexes = {
        @Index(name = "idx_current_owner_owner", columnList = "owner_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetsCurrentOwnerJpaEntity {

    @Id
    @Column(name = "pet_id", updatable = false, nullable = false)
    private UUID petId;

    @Column(name = "owner_type", length = 20)
    private String ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "caretaker_user_id")
    private UUID caretakerUserId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "acquisition_type", length = 20)
    private com.uit.petrescueapi.domain.valueobject.AcquisitionType acquisitionType;

    @Column(name = "adoption_application_id")
    private UUID adoptionApplicationId;
}
