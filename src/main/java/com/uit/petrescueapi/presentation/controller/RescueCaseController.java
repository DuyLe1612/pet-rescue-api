package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.rescue.*;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.query.RescueCaseQueryPort;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.RescueCaseWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue-cases")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Rescue Cases", description = "Rescue case management")
public class RescueCaseController {

    private final RescueCaseCommandPort commandPort;
    private final RescueCaseQueryPort queryPort;
    private final RescueCaseWebMapper mapper;

    @PostMapping
    @Operation(summary = "Report a new rescue case")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> report(
            @Valid @RequestBody CreateRescueCaseRequestDto cmd,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(commandPort.report(cmd, userId))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a rescue case")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRescueCaseRequestDto cmd) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.update(id, cmd))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change rescue case status")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRescueCaseStatusRequestDto cmd) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.changeStatus(id, cmd))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rescue case by ID")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all rescue cases")
    public ResponseEntity<ApiResponse<PageResponse<RescueCaseSummaryResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(PageRequest.of(page, size)))));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find rescue cases near a location")
    public ResponseEntity<ApiResponse<PageResponse<RescueCaseSummaryResponseDto>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") double distance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findNearby(lat, lng, distance, PageRequest.of(page, size)))));
    }

    @GetMapping("/bounding-box")
    @Operation(summary = "Find rescue cases within bounding box")
    public ResponseEntity<ApiResponse<PageResponse<RescueCaseSummaryResponseDto>>> getInBoundingBox(
            @RequestParam double minLat,
            @RequestParam double minLng,
            @RequestParam double maxLat,
            @RequestParam double maxLng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findWithinBoundingBox(minLat, minLng, maxLat, maxLng, PageRequest.of(page, size)))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAP MARKER ENDPOINTS - Ultra-fast for real-time map rendering
    // Returns lightweight markers (no pagination - limited to 500 per request)
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/map/markers")
    @Operation(summary = "Get map markers in bounding box",
            description = "Ultra-lightweight endpoint for map rendering. Returns up to 500 markers with minimal data (~100 bytes each).")
    public ResponseEntity<ApiResponse<List<RescueMapMarkerDto>>> getMapMarkers(
            @RequestParam double minLat,
            @RequestParam double minLng,
            @RequestParam double maxLat,
            @RequestParam double maxLng,
            @RequestParam(required = false) List<RescueCaseStatus> status,
            @RequestParam(required = false) List<RescuePriority> priority,
            @RequestParam(required = false) String species) {
        
        List<RescueMapMarkerDto> markers;
        if ((status == null || status.isEmpty()) && (priority == null || priority.isEmpty()) && species == null) {
            markers = queryPort.findMarkersInBounds(minLat, minLng, maxLat, maxLng);
        } else {
            markers = queryPort.findMarkersWithFilters(minLat, minLng, maxLat, maxLng, status, priority, species);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(markers));
    }
}
