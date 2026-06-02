package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.admin.DashboardOverviewDto;
import com.uit.petrescueapi.application.port.out.DashboardDataPort;
import com.uit.petrescueapi.application.port.query.AdminDashboardQueryPort;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardUsecase implements AdminDashboardQueryPort {

    private final DashboardDataPort dashboardDataPort;

    @Override
    public DashboardOverviewDto getOverview() {
        log.info("getOverview...");
        return dashboardDataPort.getOverview();
    }
}
