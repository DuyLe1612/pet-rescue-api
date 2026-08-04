package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.admin.dashboard.*;
import com.uit.petrescueapi.application.port.out.DashboardDataPort;
import com.uit.petrescueapi.application.port.query.AdminDashboardQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardUsecase implements AdminDashboardQueryPort {

    private final DashboardDataPort dashboardDataPort;

    @Override
    @Cacheable(value = "admin:dashboard", key = "'overview:' + #month + '-' + #year")
    public SystemDashboardOverviewDto getOverview(int month, int year) {
        log.info("getOverview for {}/{}", month, year);
        return dashboardDataPort.getOverview(month, year);
    }

    @Override
    @Cacheable(value = "admin:dashboard", key = "'adoption-trend:' + #year")
    public AdoptionTrendDto getAdoptionTrend(int year) {
        log.info("getAdoptionTrend for year {}", year);
        return dashboardDataPort.getAdoptionTrend(year);
    }

    @Override
    @Cacheable(value = "admin:rescue-stats", key = "'rescue-summary:' + #month + '-' + #year")
    public RescueSummaryDto getRescueSummary(int month, int year) {
        log.info("getRescueSummary for {}/{}", month, year);
        return dashboardDataPort.getRescueSummary(month, year);
    }

    @Override
    @Cacheable(value = "admin:dashboard", key = "'top-orgs:' + #month + '-' + #year + '-' + #limit")
    public TopOrganizationsDto getTopOrganizations(int month, int year, int limit) {
        log.info("getTopOrganizations for {}/{} limit {}", month, year, limit);
        return dashboardDataPort.getTopOrganizations(month, year, limit);
    }

    @Override
    @Cacheable(value = "admin:pending-tasks", key = "'pending-tasks:' + #month + '-' + #year")
    public PendingTasksDto getPendingTasks(int month, int year) {
        log.info("getPendingTasks for {}/{}", month, year);
        return dashboardDataPort.getPendingTasks(month, year);
    }

    @CacheEvict(value = {"admin:dashboard", "admin:rescue-stats", "admin:pending-tasks"}, allEntries = true)
    public void evictAllCaches() {
        log.info("Evicting all admin dashboard caches");
    }

    @CacheEvict(value = "admin:dashboard", allEntries = true)
    public void evictDashboardCache() {
        log.info("Evicting admin:dashboard cache");
    }

    @CacheEvict(value = "admin:rescue-stats", allEntries = true)
    public void evictRescueStatsCache() {
        log.info("Evicting admin:rescue-stats cache");
    }

    @CacheEvict(value = "admin:pending-tasks", allEntries = true)
    public void evictPendingTasksCache() {
        log.info("Evicting admin:pending-tasks cache");
    }
}
