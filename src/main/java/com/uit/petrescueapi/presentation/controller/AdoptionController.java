package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.adoption.*;
import com.uit.petrescueapi.application.port.command.AdoptionCommandPort;
import com.uit.petrescueapi.application.port.query.AdoptionQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.AdoptionWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/adoptions")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Adoptions", description = "Adoption application management")
public class AdoptionController {

    private final AdoptionCommandPort commandPort;
    private final AdoptionQueryPort queryPort;
    private final AdoptionWebMapper mapper;

    @PostMapping
    @Operation(summary = "Submit an adoption application")
    public ResponseEntity<ApiResponse<AdoptionResponseDto>> submit(
            @Valid @RequestBody CreateAdoptionRequestDto cmd,
            Authentication authentication) {
        UUID applicantId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(commandPort.submit(cmd, applicantId))));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve an adoption application")
    public ResponseEntity<ApiResponse<AdoptionResponseDto>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequestDto decision,
            Authentication authentication) {
        UUID decidedBy = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.approve(id, decision, decidedBy))));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject an adoption application")
    public ResponseEntity<ApiResponse<AdoptionResponseDto>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequestDto decision,
            Authentication authentication) {
        UUID decidedBy = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.reject(id, decision, decidedBy))));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an adoption application")
    public ResponseEntity<ApiResponse<AdoptionResponseDto>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.cancel(id))));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete an approved adoption", 
               description = "Completes an adoption that has been approved. This transfers pet ownership to the adopter and marks the pet as ADOPTED.")
    public ResponseEntity<ApiResponse<AdoptionResponseDto>> complete(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID completedBy = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.complete(id, completedBy))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get adoption application by ID")
    public ResponseEntity<ApiResponse<AdoptionResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all adoption applications")
    public ResponseEntity<ApiResponse<PageResponse<AdoptionSummaryResponseDto>>> getAll(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(status, search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get adoption applications by user ID (applicant)")
    public ResponseEntity<ApiResponse<PageResponse<AdoptionSummaryResponseDto>>> getByUserId(
            @PathVariable UUID userId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findByApplicantId(userId, status, search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }

    @GetMapping("/organization/{orgId}")
    @Operation(summary = "Get adoption applications by organization ID")
    public ResponseEntity<ApiResponse<PageResponse<AdoptionSummaryResponseDto>>> getByOrganizationId(
            @PathVariable UUID orgId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findByOrganizationId(orgId, status, search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }
}
