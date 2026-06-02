package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.admin.DashboardOverviewDto;

public interface DashboardDataPort {
    DashboardOverviewDto getOverview();
}
