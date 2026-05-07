package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.TransferOwnershipRequestDto;
import com.uit.petrescueapi.application.dto.pet.UpdatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.application.port.query.PetQueryPort;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.PetWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing Pet CRUD endpoints.
 *
 * <p>Uses CQRS: commands go through {@link PetCommandPort},
 * queries go through {@link PetQueryPort} which returns DTOs directly
 * (organization data included via JOIN — no extra flags needed).</p>
 */
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pets", description = "Pet CRUD operations")
public class PetController {

    private final PetCommandPort petCommandPort;
    private final PetQueryPort petQueryPort;
    private final PetWebMapper mapper;

    // ── Commands (write) ─────────────────────────

    @PostMapping
    @Operation(summary = "Create a pet as a regular user (user-owned)")
    public ResponseEntity<ApiResponse<PetResponseDto>> createAsUser(
            @Valid @RequestBody CreatePetRequestDto cmd,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        log.debug("Creating pet for user {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(petCommandPort.createForUser(cmd, userId))));
    }

    @PostMapping("/shelter")
    @Operation(summary = "Create a pet as a shelter member (organization-owned)")
    public ResponseEntity<ApiResponse<PetResponseDto>> createAsShelter(
            @Valid @RequestBody CreatePetRequestDto cmd,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        // shelterId must be provided in request body
        UUID shelterId = cmd.getShelterId();
        if (shelterId == null) {
            throw new ForbiddenException("shelterId is required for shelter pet creation");
        }

        log.debug("Creating pet for shelter {} by user {}", shelterId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(petCommandPort.createForShelter(cmd, shelterId, userId))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing pet")
    public ResponseEntity<ApiResponse<PetResponseDto>> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdatePetRequestDto cmd) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(petCommandPort.update(id, cmd))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change pet status")
    public ResponseEntity<ApiResponse<PetResponseDto>> changeStatus(@PathVariable UUID id,
                                                             @RequestParam PetStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(petCommandPort.changeStatus(id, status))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a pet")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        petCommandPort.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Pet deleted"));
    }

    @PostMapping("/{id}/transfer-ownership")
    @Operation(summary = "Transfer pet ownership (Admin or Organization Owner only)",
               description = "Manually transfer pet ownership to a new user or organization. " +
                           "Only system admins or owners of the organization that currently owns the pet can perform this action.")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequestDto cmd,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        log.debug("Transferring ownership of pet {} to {} {} by user {}",
                id, cmd.getNewOwnerType(), cmd.getNewOwnerId(), requesterId);
        petCommandPort.transferOwnership(id, cmd, requesterId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Ownership transferred successfully"));
    }

    // ── Queries (read) ──────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get pet by ID (includes organization details)")
    public ResponseEntity<ApiResponse<PetResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(petQueryPort.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all pets (paginated, with optional filters)")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) java.util.List<String> status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID organizationId) {
        java.util.List<PetStatus> statusList = status == null ? null : 
                status.stream().map(this::parsePetStatus).toList();
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findAllWithFilters(
                        species, breed, gender, searchName, statusList, userId, organizationId, PageRequest.of(page, size)
                ))));
    }

    @GetMapping("/available")
    @Operation(summary = "List available pets (paginated, with optional filters)")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findAvailableWithFilters(
                        species, breed, gender, organizationId, PageRequest.of(page, size)
                ))));
    }

    @GetMapping("/by-organization/{organizationId}")
    @Operation(summary = "List pets owned by an organization (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getByOrganization(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findByOrganizationId(
                        organizationId, species, breed, gender, PageRequest.of(page, size)
                ))));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "List pets owned by a user (paginated, with optional filters)")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findByUserId(
                        userId, species, breed, gender, PageRequest.of(page, size)
                ))));
    }

        private PetStatus parsePetStatus(String value) {
                try {
                        return PetStatus.valueOf(value);
                } catch (IllegalArgumentException ex) {
                        throw new BusinessException("Invalid pet status: " + value, "INVALID_PET_STATUS");
                }
        }
}
