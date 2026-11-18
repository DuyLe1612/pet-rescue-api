package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

/** A pet in the owner's own list: the usual summary plus how they came to have it. */
public interface MyPetProjection extends PetSummaryProjection {
    String getAcquisitionType();
    UUID getAdoptionApplicationId();
    /** The organization the pet was adopted from; null for a pet the person registered. */
    UUID getSourceOrganizationId();
    String getSourceOrganizationName();
}
