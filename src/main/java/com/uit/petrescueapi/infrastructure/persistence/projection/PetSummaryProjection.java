package com.uit.petrescueapi.infrastructure.persistence.projection;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;

import java.util.UUID;

/**
 * Spring Data interface projection for the pet summary list query.
 */
public interface PetSummaryProjection {

    // ── Pet fields ──────────────────────────────
    UUID getId();
    String getPetCode();
    String getName();
    UUID getSpeciesId();
    String getSpecies();
    UUID getBreedId();
    String getBreed();
    Integer getAge();
    boolean getVaccinated();
    Gender getGender();
    PetStatus getStatus();
    HealthStatus getHealthStatus();
    String getImagePublicId();
    String getOwnerType();
    UUID getOwnerId();
    String getOwnerName();
    String getOwnerAvatarUrl();
    String getOwnerPhone();
    UUID getCaretakerUserId();
    String getCaretakerName();
    String getCaretakerAvatarUrl();
    String getCaretakerPhone();

    // ── Organization fields (for nested OrganizationMinimalDto) ─
    UUID getOrganizationId();
    String getOrganizationName();
    String getOrganizationUrl();

    // ── Location fields (from organization) ─
    String getProvinceName();
    Integer getProvinceCode();
    String getWardName();
    Integer getWardCode();
}
