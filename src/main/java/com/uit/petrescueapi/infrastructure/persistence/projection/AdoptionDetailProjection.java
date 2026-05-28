package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data interface projection for adoption application detail queries.
 *
 * <p>Returns all adoption fields plus pet, applicant, and organization data
 * via LEFT JOIN.</p>
 */
public interface AdoptionDetailProjection {

    // ── Summary fields ───────────────────────────
    UUID getApplicationId();
    String getAdoptionCode();
    String getPetName();
    String getPetPrimaryImageUrl();
    String getApplicantUsername();
    String getStatus();
    String getExperience();
    String getLiveCondition();
    LocalDateTime getCreatedAt();

    // ── Detail fields ────────────────────────────
    UUID getPetId();
    UUID getApplicantId();
    UUID getOrganizationId();
    String getOrganizationName();
    String getNote();
    LocalDateTime getDecidedAt();
    UUID getDecidedBy();
    String getDecidedByUsername();
    String getRejectReason();
    LocalDateTime getReadyAt();
}
