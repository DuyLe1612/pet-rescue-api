package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

public interface TopOrganizationProjection {
    UUID getOrganizationId();
    String getOrganizationName();
    String getStatus();
    Long getTotalPets();
    Long getOpenRescueCases();
    Long getPendingAdoptions();
    Long getCompletedAdoptions();
    Long getHealthAlerts();
}
