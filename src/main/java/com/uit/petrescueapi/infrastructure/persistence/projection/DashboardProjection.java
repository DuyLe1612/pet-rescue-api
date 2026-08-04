package com.uit.petrescueapi.infrastructure.persistence.projection;

public interface DashboardProjection {
    Long getTotalOrganizations();
    Long getPendingOrganizations();
    Long getPrevTotalOrganizations();
    Long getPrevPendingOrganizations();
    Long getPrevTotalPets();
    Long getTotalPets();
    Long getPrevPendingPets();
    Long getOpenRescueCases();
    Long getPrevOpenRescueCases();
    Long getPendingAdoptions();
    Long getCompletedAdoptions();
    Long getPrevPendingAdoptions();
    Long getPrevCompletedAdoptions();
}
