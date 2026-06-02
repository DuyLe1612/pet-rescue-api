package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.admin.DashboardOverviewDto;

public interface AdminDashboardQueryPort {
    DashboardOverviewDto getOverview();
}
