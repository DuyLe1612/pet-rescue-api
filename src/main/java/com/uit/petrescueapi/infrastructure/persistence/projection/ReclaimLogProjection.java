package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** A reclaim log with the names of the pet, organization and people it refers to. */
public interface ReclaimLogProjection {

    UUID getReclaimId();
    UUID getApplicationId();
    UUID getPetId();
    String getPetName();
    UUID getOrganizationId();
    String getOrganizationName();
    UUID getUserId();
    String getUsername();
    UUID getReclaimedBy();
    String getReclaimedByUsername();
    String getReason();
    UUID getProofId();
    Integer getReputationDelta();
    LocalDateTime getReclaimedAt();
}
