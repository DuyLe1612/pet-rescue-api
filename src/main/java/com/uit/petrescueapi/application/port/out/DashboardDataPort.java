package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.admin.dashboard.*;

public interface DashboardDataPort {
    SystemDashboardOverviewDto getOverview(int month, int year);
    AdoptionTrendDto getAdoptionTrend(int year);
    RescueSummaryDto getRescueSummary(int month, int year);
    TopOrganizationsDto getTopOrganizations(int month, int year, int limit);
    PendingTasksDto getPendingTasks(int month, int year);
}
