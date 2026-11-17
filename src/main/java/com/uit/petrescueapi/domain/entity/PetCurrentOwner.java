package com.uit.petrescueapi.domain.entity;

import lombok.*;

import java.util.UUID;

/**
 * PetCurrentOwner — represents the current owner of a pet.
 * Pure domain entity: no JPA annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetCurrentOwner {

    private UUID petId;
    private String ownerType;
    private UUID ownerId;
    private UUID caretakerUserId;

    private com.uit.petrescueapi.domain.valueobject.AcquisitionType acquisitionType;
    private UUID adoptionApplicationId;
}
