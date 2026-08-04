package com.uit.petrescueapi.application.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopOrganizationItemDto implements Serializable {
    private UUID organizationId;
    private String organizationName;
    private String status;
    private long totalPets;
    private long openRescueCases;
    private long pendingAdoptions;
    private long completedAdoptions;
    private long healthAlerts;
}
