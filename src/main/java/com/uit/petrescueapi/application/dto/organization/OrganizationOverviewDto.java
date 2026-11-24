package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What a centre has done and has on its hands. The public part is what someone choosing a centre
 * wants to know; {@code operations} is only filled for admins and the centre's own members.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Centre overview: public figures, plus day-to-day work for admins and members")
public class OrganizationOverviewDto {

    private UUID organizationId;

    @Schema(description = "Pets waiting for a home (AVAILABLE)")
    private long availablePets;

    @Schema(description = "Pets the centre is caring for now: every status except ADOPTED and DECEASED")
    private long petsInCare;

    @Schema(description = "Adoptions completed through this centre")
    private long adoptedPets;

    @Schema(description = "Rescue cases the centre took and completed")
    private long rescuedCases;

    @Schema(description = "Only for admins and members of this centre; null otherwise")
    private Operations operations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Operations {
        private String organizationCode;
        @Schema(description = "Display name of the person who registered the centre; null when an admin created it")
        private String requestedByName;
        private LocalDateTime createdAt;
        private long pendingAdoptions;
        @Schema(description = "Approved applications waiting for the adopter to pick the pet up")
        private long awaitingPickup;
        @Schema(description = "Rescue cases the centre has claimed and is working on")
        private long activeRescueClaims;
        private long petsUnderTreatment;
        @Schema(description = "Post-adoption alerts not resolved yet")
        private long openFollowupAlerts;
        private long activeMembers;
    }
}
