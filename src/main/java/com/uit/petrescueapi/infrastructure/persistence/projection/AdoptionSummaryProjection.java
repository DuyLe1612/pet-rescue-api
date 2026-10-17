package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data interface projection for adoption application summary list queries.
 *
 * <p>Maps from JPQL {@code SELECT ... FROM adoption_applications a LEFT JOIN pets p LEFT JOIN users u}
 * query. Column aliases must match getter names (camelCase).</p>
 */
public interface AdoptionSummaryProjection {

    UUID getApplicationId();
    String getAdoptionCode();
    UUID getPetId();
    String getPetName();
    String getPetPrimaryImageUrl();
    String getApplicantUsername();
    String getStatus();
    String getExperience();
    String getLiveCondition();
    LocalDateTime getCreatedAt();
}
