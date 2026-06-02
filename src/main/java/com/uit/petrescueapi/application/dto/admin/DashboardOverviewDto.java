package com.uit.petrescueapi.application.dto.admin;

public record DashboardOverviewDto(
        UserStatsDto users,

        PetStatsDto pets,

        OrganizationStatsDto organizations
) {
}