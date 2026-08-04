package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.admin.dashboard.*;
import com.uit.petrescueapi.application.port.query.AdminDashboardQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Dashboard operations for admin")
public class AdminDashboardController {

    private final AdminDashboardQueryPort adminDashboardQueryPort;

    @GetMapping("/overview")
    @Operation(summary = "Get system dashboard overview")
    public ResponseEntity<ApiResponse<SystemDashboardOverviewDto>> getDashboardOverview(
            @RequestParam int month,
            @RequestParam int year) {
        SystemDashboardOverviewDto data = adminDashboardQueryPort.getOverview(month, year);
        return ResponseEntity.ok(ApiResponse.ok(data, "Get system dashboard overview successfully"));
    }

    @GetMapping("/adoption-trend")
    @Operation(summary = "Get adoption trend")
    public ResponseEntity<ApiResponse<AdoptionTrendDto>> getAdoptionTrend(
            @RequestParam int year) {
        AdoptionTrendDto data = adminDashboardQueryPort.getAdoptionTrend(year);
        return ResponseEntity.ok(ApiResponse.ok(data, "Get adoption trend successfully"));
    }

    @GetMapping("/rescue-summary")
    @Operation(summary = "Get rescue summary")
    public ResponseEntity<ApiResponse<RescueSummaryDto>> getRescueSummary(
            @RequestParam int month,
            @RequestParam int year) {
        RescueSummaryDto data = adminDashboardQueryPort.getRescueSummary(month, year);
        return ResponseEntity.ok(ApiResponse.ok(data, "Get rescue summary successfully"));
    }

    @GetMapping("/top-organizations")
    @Operation(summary = "Get top organizations")
    public ResponseEntity<ApiResponse<TopOrganizationsDto>> getTopOrganizations(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "10") int limit) {
        TopOrganizationsDto data = adminDashboardQueryPort.getTopOrganizations(month, year, limit);
        return ResponseEntity.ok(ApiResponse.ok(data, "Get top organizations successfully"));
    }

    @GetMapping("/pending-tasks")
    @Operation(summary = "Get pending tasks")
    public ResponseEntity<ApiResponse<PendingTasksDto>> getPendingTasks(
            @RequestParam int month,
            @RequestParam int year) {
        PendingTasksDto data = adminDashboardQueryPort.getPendingTasks(month, year);
        return ResponseEntity.ok(ApiResponse.ok(data, "Get pending tasks successfully"));
    }
}
