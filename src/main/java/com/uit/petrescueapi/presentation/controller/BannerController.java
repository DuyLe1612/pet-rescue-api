package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.banner.BannerResponseDto;
import com.uit.petrescueapi.application.dto.banner.CreateBannerRequestDto;
import com.uit.petrescueapi.application.dto.banner.UpdateBannerRequestDto;
import com.uit.petrescueapi.application.port.command.BannerCommandPort;
import com.uit.petrescueapi.application.port.query.BannerQueryPort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.BannerWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Banner CRUD operations.
 *
 * Public endpoints:
     *  - GET /active?targetPage=HOME - Get active banners for display
 *
 * Admin endpoints (require ADMIN role):
 *  - All other CRUD operations
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banners", description = "Banner management for landing page")
public class BannerController {

    private final BannerCommandPort commandPort;
    private final BannerQueryPort queryPort;
    private final BannerWebMapper mapper;

    // ── Public Endpoints ─────────────────────────────

    @GetMapping("/active")
    @Operation(summary = "Get active banners by query params (public)",
               description = "Returns banners that are active, within date range, sorted by display order")
    public ResponseEntity<ApiResponse<List<BannerResponseDto>>> getActiveBanners(
            @RequestParam(defaultValue = "HOME") String targetPage) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findActiveByTargetPage(targetPage)));
    }

    // ── Admin Endpoints ──────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new banner (Admin only)")
    public ResponseEntity<ApiResponse<BannerResponseDto>> create(
            @Valid @RequestBody CreateBannerRequestDto cmd) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(commandPort.create(cmd))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing banner (Admin only)")
    public ResponseEntity<ApiResponse<BannerResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBannerRequestDto cmd) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.update(id, cmd))));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle banner active status (Admin only)")
    public ResponseEntity<ApiResponse<BannerResponseDto>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.toggleActive(id))));
    }

    @PatchMapping("/{id}/display-order")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update banner display order (Admin only)")
    public ResponseEntity<ApiResponse<BannerResponseDto>> updateDisplayOrder(
            @PathVariable UUID id,
            @RequestParam Integer order) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.updateDisplayOrder(id, order))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a banner (Admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        commandPort.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Banner deleted"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get banner by ID (Admin only)")
    public ResponseEntity<ApiResponse<BannerResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all banners (Admin only, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<BannerResponseDto>>> getAll(
            @RequestParam(required = false) String targetPage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Boolean activeFilter = parseStatusFilter(status);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAllFiltered(targetPage, activeFilter, search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }

    private Boolean parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return true;
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return false;
        }
        throw new BusinessException("Invalid banner status filter. Use ACTIVE or INACTIVE", "INVALID_BANNER_STATUS");
    }
}
