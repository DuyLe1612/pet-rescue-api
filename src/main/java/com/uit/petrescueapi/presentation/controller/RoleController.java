package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.role.*;
import com.uit.petrescueapi.application.port.command.RoleCommandPort;
import com.uit.petrescueapi.application.port.query.RoleQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.RoleWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Roles", description = "Role and permission management")
public class RoleController {

    private final RoleCommandPort commandPort;
    private final RoleQueryPort queryPort;
    private final RoleWebMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new role")
    public ResponseEntity<ApiResponse<RoleResponseDto>> create(
            @Valid @RequestBody CreateRoleRequestDto cmd) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(commandPort.create(cmd))));
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "Assign permissions to a role")
    public ResponseEntity<ApiResponse<RoleResponseDto>> assignPermissions(
            @PathVariable Integer id,
            @Valid @RequestBody AssignPermissionsRequestDto cmd) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.assignPermissions(id, cmd))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        commandPort.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping
    @Operation(summary = "List all roles")
    public ResponseEntity<ApiResponse<PageResponse<RoleSummaryResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleResponseDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "Get permissions for a role")
    public ResponseEntity<ApiResponse<List<PermissionResponseDto>>> getPermissions(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.getPermissions(id)));
    }
}
