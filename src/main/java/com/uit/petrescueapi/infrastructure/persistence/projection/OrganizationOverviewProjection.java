package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row of counts for one centre; see {@code OrganizationQueryJpaRepository#findOverview}. */
public interface OrganizationOverviewProjection {
    UUID getOrganizationId();
    String getOrganizationCode();
    String getRequestedByName();
    LocalDateTime getCreatedAt();
    Long getAvailablePets();
    Long getPetsInCare();
    Long getPetsUnderTreatment();
    Long getAdoptedPets();
    Long getPendingAdoptions();
    Long getAwaitingPickup();
    Long getActiveRescueClaims();
    Long getRescuedCases();
    Long getOpenFollowupAlerts();
    Long getActiveMembers();
}
