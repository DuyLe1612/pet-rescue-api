package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.PetBreedResponseDto;
import com.uit.petrescueapi.application.dto.pet.TransferOwnershipRequestDto;
import com.uit.petrescueapi.application.dto.pet.UpdatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSpeciesResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.application.port.query.PetQueryPort;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.PetWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetBreedJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetSpeciesJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final UserDomainService userDomainService;
    private final OrganizationDomainService organizationDomainService;
    private final PetSpeciesJpaRepository speciesRepository;
    private final PetBreedJpaRepository breedRepository;

    // ── Commands (write) ─────────────────────────

    @PostMapping
    @Operation(summary = "Create a pet as a regular user (user-owned)")
    public ResponseEntity<ApiResponse<PetResponseDto>> createAsUser(
            @Valid @RequestBody CreatePetRequestDto cmd,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        if (cmd.getShelterId() != null) {
            log.debug("Creating pet for shelter {} by user {}", cmd.getShelterId(), userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(mapper.toDto(petCommandPort.createForShelter(cmd, cmd.getShelterId(), userId))));
        }

        log.debug("Creating pet for user {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(petCommandPort.createForUser(cmd, userId))));
    }

    @PostMapping("/shelter")
    @Deprecated
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

    @GetMapping("/species")
    @Operation(summary = "List pet species")
    public ResponseEntity<ApiResponse<PageResponse<PetSpeciesResponseDto>>> getSpecies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        var pageable = PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder);
        var species = (search == null || search.isBlank()
                ? speciesRepository.findByDeletedFalse(pageable)
                : speciesRepository.findByNameContainingIgnoreCaseAndDeletedFalse(search, pageable))
                .map(item -> PetSpeciesResponseDto.builder()
                        .speciesId(item.getSpeciesId())
                        .name(item.getName())
                        .build());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(species)));
    }

    @GetMapping("/species/{speciesId}/breeds")
    @Operation(summary = "List pet breeds by species")
    public ResponseEntity<ApiResponse<PageResponse<PetBreedResponseDto>>> getBreedsBySpecies(
            @PathVariable UUID speciesId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        var pageable = PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder);
        var breeds = (search == null || search.isBlank()
                ? breedRepository.findBySpeciesIdAndDeletedFalse(speciesId, pageable)
                : breedRepository.findBySpeciesIdAndNameContainingIgnoreCaseAndDeletedFalse(speciesId, search, pageable))
                .map(item -> PetBreedResponseDto.builder()
                        .breedId(item.getBreedId())
                        .speciesId(item.getSpeciesId())
                        .name(item.getName())
                        .build());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(breeds)));
    }

    @GetMapping
    @Operation(summary = "List all pets (paginated, with optional filters)",
            description = "Unified endpoint for listing pets. Supports filtering by status, user, organization, and availability. " +
                    "Use status=AVAILABLE or availableOnly=true for available pets. " +
                    "Use organizationId=X to filter by organization. Use userId=X to filter by owner.")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Boolean availableOnly) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        UUID requesterId = null;
        boolean admin = false;

        if (isAuthenticated) {
            try {
                requesterId = UUID.fromString(authentication.getName());
                admin = userDomainService.hasRole(requesterId, "ADMIN");
            } catch (IllegalArgumentException e) {
                requesterId = null;
            }
        }

        boolean canUseStatus = admin || (requesterId != null && canUseOrgScopedPetStatusFilter(organizationId, requesterId));

        if (!admin) {
            userId = null;
        }
        if (!canUseStatus) {
            status = null;
        }

        List<PetStatus> statusList = status == null ? null :
                status.stream().map(this::parsePetStatus).toList();

        if (Boolean.TRUE.equals(availableOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(
                    PageResponse.from(petQueryPort.findAvailableWithFilters(
                            species, breed, gender, organizationId, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)
                    ))));
        }

        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findAllWithFilters(
                        species, breed, gender, search, statusList, userId, organizationId, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)
                ))));
    }

    @GetMapping("/available")
    @Deprecated
    @Operation(summary = "List available pets (paginated, with optional filters)",
            description = "Deprecated: Use GET /api/v1/pets with status=AVAILABLE query parameter instead")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findAvailableWithFilters(
                        species, breed, gender, organizationId, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)
                ))));
    }

    @GetMapping("/by-organization/{organizationId}")
    @Deprecated
    @Operation(summary = "List pets owned by an organization (paginated)",
            description = "Deprecated: Use GET /api/v1/pets with organizationId={id} query parameter instead")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getByOrganization(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findByOrganizationId(
                        organizationId, species, breed, gender, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)
                ))));
    }

    @GetMapping("/by-user/{userId}")
    @Deprecated
    @Operation(summary = "List pets owned by a user (paginated, with optional filters)",
            description = "Deprecated: Use GET /api/v1/pets with userId={id} query parameter instead")
    public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(petQueryPort.findByUserId(
                        userId, species, breed, gender, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)
                ))));
    }

        private PetStatus parsePetStatus(String value) {
                try {
                        return PetStatus.valueOf(value);
                } catch (IllegalArgumentException ex) {
                        throw new BusinessException("Invalid pet status: " + value, "INVALID_PET_STATUS");
                }
        }

        private boolean canUseOrgScopedPetStatusFilter(UUID organizationId, UUID userId) {
                if (organizationId == null) {
                        return false;
                }
                return organizationDomainService.getMemberRole(organizationId, userId)
                        .map(role -> "OWNER".equals(role) || "STAFF".equals(role) || "VET".equals(role))
                        .orElse(false);
        }
}
