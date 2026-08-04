package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.admin.dashboard.*;
import com.uit.petrescueapi.application.port.out.DashboardDataPort;
import com.uit.petrescueapi.application.support.trend.TrendCalculator;
import com.uit.petrescueapi.infrastructure.persistence.projection.DashboardProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.AdminDashboardProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardQueryAdapter implements DashboardDataPort {

    private final AdminDashboardProjectionRepository projectionRepository;

    @Override
    public SystemDashboardOverviewDto getOverview(int month, int year) {
        LocalDateTime[] range = monthRange(month, year);
        LocalDateTime[] prevRange = monthRangePrev(month, year);

        DashboardProjection proj = projectionRepository.getDashboardOverview(
                range[0], range[1], prevRange[0], prevRange[1]);

        return SystemDashboardOverviewDto.builder()
                .period(new PeriodDto(month, year))
                .kpis(SystemKpisDto.builder()
                        .totalOrganizations(new KpiItemDto(proj.getTotalOrganizations(),
                                TrendCalculator.calculatePercentageChange(proj.getTotalOrganizations(), proj.getPrevTotalOrganizations()),
                                TrendCalculator.calculateTrend(proj.getTotalOrganizations(), proj.getPrevTotalOrganizations())))
                        .pendingOrganizations(new KpiItemDto(proj.getPendingOrganizations(),
                                TrendCalculator.calculatePercentageChange(proj.getPendingOrganizations(), proj.getPrevPendingOrganizations()),
                                TrendCalculator.calculateTrend(proj.getPendingOrganizations(), proj.getPrevPendingOrganizations())))
                        .totalPets(new KpiItemDto(proj.getTotalPets(),
                                TrendCalculator.calculatePercentageChange(proj.getTotalPets(), proj.getPrevTotalPets()),
                                TrendCalculator.calculateTrend(proj.getTotalPets(), proj.getPrevTotalPets())))
                        .openRescueCases(new KpiItemDto(proj.getOpenRescueCases(),
                                TrendCalculator.calculatePercentageChange(proj.getOpenRescueCases(), proj.getPrevOpenRescueCases()),
                                TrendCalculator.calculateTrend(proj.getOpenRescueCases(), proj.getPrevOpenRescueCases())))
                        .pendingAdoptions(new KpiItemDto(proj.getPendingAdoptions(),
                                TrendCalculator.calculatePercentageChange(proj.getPendingAdoptions(), proj.getPrevPendingAdoptions()),
                                TrendCalculator.calculateTrend(proj.getPendingAdoptions(), proj.getPrevPendingAdoptions())))
                        .completedAdoptions(new KpiItemDto(proj.getCompletedAdoptions(),
                                TrendCalculator.calculatePercentageChange(proj.getCompletedAdoptions(), proj.getPrevCompletedAdoptions()),
                                TrendCalculator.calculateTrend(proj.getCompletedAdoptions(), proj.getPrevCompletedAdoptions())))
                        .build())
                .build();
    }

    @Override
    public AdoptionTrendDto getAdoptionTrend(int year) {
        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year + 1, 1, 1, 0, 0, 0);

        var projections = projectionRepository.getAdoptionTrend(yearStart, yearEnd);
        List<AdoptionTrendItemDto> items = projections.stream()
                .map(p -> new AdoptionTrendItemDto(
                        p.getMonth(),
                        "T" + p.getMonth(),
                        p.getPending(),
                        p.getApproved(),
                        p.getRejected(),
                        p.getCompleted()))
                .toList();

        return AdoptionTrendDto.builder()
                .year(year)
                .chartType("line")
                .items(items)
                .build();
    }

    @Override
    public RescueSummaryDto getRescueSummary(int month, int year) {
        LocalDateTime[] range = monthRange(month, year);

        var statusProjs = projectionRepository.getRescueSummaryByStatus(range[0], range[1]);
        var priorityProjs = projectionRepository.getRescueSummaryByPriority(range[0], range[1]);

        return RescueSummaryDto.builder()
                .period(new PeriodDto(month, year))
                .byStatus(statusProjs.stream()
                        .map(p -> new StatusSummaryItemDto(p.getStatus(), mapStatusLabel(p.getStatus()), p.getCount()))
                        .toList())
                .byPriority(priorityProjs.stream()
                        .map(p -> new PrioritySummaryItemDto(p.getPriority(), mapPriorityLabel(p.getPriority()), p.getCount()))
                        .toList())
                .build();
    }

    private String mapStatusLabel(String status) {
        return switch (status) {
            case "REPORTED" -> "Mới báo cáo";
            case "IN_PROGRESS" -> "Đang xử lý";
            case "RESCUED" -> "Đã cứu trợ";
            case "CLOSED" -> "Đã đóng";
            default -> status;
        };
    }

    private String mapPriorityLabel(String priority) {
        return switch (priority) {
            case "LOW" -> "Thấp";
            case "MEDIUM" -> "Trung bình";
            case "HIGH" -> "Cao";
            case "CRITICAL" -> "Khẩn cấp";
            default -> priority;
        };
    }

    @Override
    public TopOrganizationsDto getTopOrganizations(int month, int year, int limit) {
        LocalDateTime[] range = monthRange(month, year);

        var projs = projectionRepository.getTopOrganizations(range[0], range[1], limit);
        return TopOrganizationsDto.builder()
                .items(projs.stream()
                        .map(p -> new TopOrganizationItemDto(
                                p.getOrganizationId(),
                                p.getOrganizationName(),
                                p.getStatus(),
                                p.getTotalPets(),
                                p.getOpenRescueCases(),
                                p.getPendingAdoptions(),
                                p.getCompletedAdoptions(),
                                p.getHealthAlerts()))
                        .toList())
                .build();
    }

    @Override
    public PendingTasksDto getPendingTasks(int month, int year) {
        LocalDateTime[] range = monthRange(month, year);

        var projs = projectionRepository.getPendingTasks(range[0], range[1]);
        return PendingTasksDto.builder()
                .items(projs.stream()
                        .map(p -> new PendingTaskItemDto(
                                p.getTaskId(),
                                p.getType(),
                                p.getTitle(),
                                p.getReferenceId(),
                                p.getStatus(),
                                p.getPriority(),
                                p.getCreatedAt(),
                                p.getActionUrl()))
                        .toList())
                .build();
    }

    private LocalDateTime[] monthRange(int month, int year) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime end = YearMonth.of(year, month).plusMonths(1).atDay(1).atStartOfDay();
        return new LocalDateTime[]{start, end};
    }

    private LocalDateTime[] monthRangePrev(int month, int year) {
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        LocalDateTime start = prev.atDay(1).atStartOfDay();
        LocalDateTime end = YearMonth.of(year, month).atDay(1).atStartOfDay();
        return new LocalDateTime[]{start, end};
    }
}
