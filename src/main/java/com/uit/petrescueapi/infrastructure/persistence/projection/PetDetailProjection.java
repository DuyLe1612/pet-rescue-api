package com.uit.petrescueapi.infrastructure.persistence.projection;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data interface projection for the pet detail query.
 */
public interface PetDetailProjection {

    // ── Pet fields ──────────────────────────────
    UUID getId();
    String getPetCode();
    String getName();
    UUID getSpeciesId();
    String getSpecies();
    UUID getBreedId();
    String getBreed();
    Integer getAge();
    Gender getGender();
    String getColor();
    BigDecimal getWeight();
    String getDescription();
    PetStatus getStatus();
    HealthStatus getHealthStatus();
    boolean getVaccinated();
    boolean getNeutered();
    LocalDate getRescueDate();
    String getRescueLocation();
    UUID getRescueCaseId();
    UUID getShelterId();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
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
