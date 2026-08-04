package com.uit.petrescueapi.application.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemKpisDto implements Serializable {
    private KpiItemDto totalOrganizations;
    private KpiItemDto pendingOrganizations;
    private KpiItemDto totalPets;
    private KpiItemDto openRescueCases;
    private KpiItemDto pendingAdoptions;
    private KpiItemDto completedAdoptions;
}
