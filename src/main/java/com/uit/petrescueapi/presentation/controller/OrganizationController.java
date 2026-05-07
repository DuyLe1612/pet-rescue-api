package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.organization.*;
import com.uit.petrescueapi.application.port.command.OrganizationCommandPort;
import com.uit.petrescueapi.application.port.query.OrganizationQueryPort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.OrganizationWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organizations", description = "Organization CRUD and membership management")
public class OrganizationController {

    private static final Set<String> ORG_ASSIGNABLE_ROLES = Set.of("STAFF", "VET");

    private final OrganizationCommandPort commandPort;
    private final OrganizationQueryPort queryPort;
    private final OrganizationWebMapper mapper;
    private final OrganizationDomainService orgDomainService;
    private final UserDomainService userDomainService;

    // ── Commands (write) ─────────────────────────

    @PostMapping
    @Operation(summary = "Create a new organization",
            description = "Admin: creates directly as ACTIVE. User: creates as PENDING (becomes OWNER when approved)")
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> create(
            @RequestBody CreateOrganizationRequestDto cmd,
            Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        if (isAdmin) {
            log.info("Admin {} creating organization '{}' (direct ACTIVE)", callerId, cmd.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(mapper.toDto(commandPort.createByAdmin(cmd))));
        } else {
            log.info("User {} creating organization '{}' (PENDING)", callerId, cmd.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(mapper.toDto(commandPort.createByUser(cmd, callerId))));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing organization")
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody CreateOrganizationRequestDto cmd) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.update(id, cmd))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change organization status (ADMIN only)",
            description = "When changing PENDING → ACTIVE, the requesting user is auto-assigned as OWNER")
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> changeStatus(
            @PathVariable UUID id,
            @RequestParam OrganizationStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.changeStatus(id, status))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an organization (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        commandPort.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Organization deactivated"));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add STAFF or VET to this organization (OWNER only)")
    public ResponseEntity<ApiResponse<OrganizationMemberResponseDto>> addMember(
            @PathVariable UUID id,
            @RequestBody AddMemberRequestDto cmd,
            Authentication authentication) {

        // Verify caller is OWNER of this org
        UUID callerId = UUID.fromString(authentication.getName());
        String callerRole = orgDomainService.getMemberRole(id, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this organization"));
        if (!"OWNER".equals(callerRole)) {
            throw new AccessDeniedException("Only the organization OWNER can add members");
        }

        // Only STAFF or VET may be assigned via this endpoint
        if (!ORG_ASSIGNABLE_ROLES.contains(cmd.getRole())) {
            throw new BusinessException("Organization can only assign STAFF or VET roles", "INVALID_ORG_ROLE");
        }

        // Target user must not be an admin account
        if (userDomainService.findById(cmd.getUserId()).hasRole("ADMIN")) {
            throw new BusinessException("Cannot assign organization membership to an admin account", "FORBIDDEN_TARGET");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toMemberDto(commandPort.addMember(id, cmd))));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member from an organization")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        commandPort.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed"));
    }

    // ── Queries (read) ──────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<OrganizationResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List organizations (paginated, optional status filter)")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationSummaryResponseDto>>> getAll(
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<OrganizationStatus> statusList = status == null ? null : 
            status.stream().map(this::parseOrganizationStatus).toList();
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(statusList, PageRequest.of(page, size)))));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List members of an organization (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationMemberResponseDto>>> getMembers(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findMembers(id, PageRequest.of(page, size)))));
    }

    @GetMapping("/map/markers")
    @Operation(summary = "Get organization markers for map", description = "Returns up to 500 organization markers with optional type and status filters")
    public ResponseEntity<ApiResponse<List<OrganizationMapMarkerDto>>> getMapMarkers(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        List<OrganizationStatus> statuses = status == null ? null : status.stream().map(this::parseOrganizationStatus).toList();
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findMapMarkers(statuses, type)));
    }

    private OrganizationStatus parseOrganizationStatus(String value) {
        try {
            return OrganizationStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid organization status: " + value, "INVALID_ORG_STATUS");
        }
    }
}
