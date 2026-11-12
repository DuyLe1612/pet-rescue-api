package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.rescue.*;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.query.RescueCaseQueryPort;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.PetWebMapper;
import com.uit.petrescueapi.presentation.mapper.RescueCaseWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
    private final PetWebMapper petMapper;
    private final com.uit.petrescueapi.application.port.command.RescueCaseSharePort sharePort;

    @PostMapping
    @Operation(summary = "Report a new rescue case",
            description = "shareAsPost=true also publishes a community post about the case (postContent is optional). "
                    + "The case is saved first: if the post fails the response still is 201, with postShareFailed=true.")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> report(
            @Valid @RequestBody CreateRescueCaseRequestDto cmd,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        com.uit.petrescueapi.domain.entity.RescueCase reported = commandPort.report(cmd, userId);
        RescueCaseResponseDto body = mapper.toDto(reported);
        if (cmd.isShareAsPost()) {
            java.util.Optional<UUID> postId = sharePort.shareAsPost(reported, cmd.getPostContent(), userId);
            body.setSharedPostId(postId.orElse(null));
            body.setPostShareFailed(postId.isEmpty());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(body));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a rescue case")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRescueCaseRequestDto cmd,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.update(id, cmd, requesterId))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change rescue case status")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRescueCaseStatusRequestDto cmd,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.changeStatus(id, cmd, requesterId))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rescue case by ID")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> getById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id, viewerId(authentication))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLAIMS
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/claim")
    @Operation(summary = "Claim a rescue case",
            description = "OWNER and STAFF claim for their organization, which locks the case. Anyone else makes a personal claim: "
                    + "it does not lock the case, expires, and needs safetyAcknowledged=true. "
                    + "An organization also uses this to accept a rescued animal waiting for handover.")
    public ResponseEntity<ApiResponse<RescueClaimResponseDto>> claim(
            @PathVariable UUID id,
            @RequestBody(required = false) ClaimRescueCaseRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(RescueClaimResponseDto.from(
                commandPort.claim(id, request, UUID.fromString(authentication.getName())))));
    }

    @PostMapping("/{id}/claim/extend")
    @Operation(summary = "Extend my personal claim")
    public ResponseEntity<ApiResponse<RescueClaimResponseDto>> extendClaim(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(RescueClaimResponseDto.from(
                commandPort.extendClaim(id, UUID.fromString(authentication.getName())))));
    }

    @PostMapping("/{id}/claim/release")
    @Operation(summary = "Give up my claim", description = "The case reopens if nobody else is working on it.")
    public ResponseEntity<ApiResponse<RescueClaimResponseDto>> releaseClaim(
            @PathVariable UUID id,
            @RequestBody(required = false) ReleaseRescueClaimRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(RescueClaimResponseDto.from(
                commandPort.releaseClaim(id, request, UUID.fromString(authentication.getName())))));
    }

    @GetMapping("/{id}/claims")
    @Operation(summary = "Who is working on this case", description = "For the reporter, the handling organization and admins.")
    public ResponseEntity<ApiResponse<List<RescueClaimResponseDto>>> getClaims(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findClaimsOfCase(id, viewerId(authentication))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AFTER THE RESCUE
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/intake")
    @Operation(summary = "Create the pet record for a rescued case held by my organization",
            description = "The pet starts in FOSTERING. Fails if the case already has a pet.")
    public ResponseEntity<ApiResponse<PetResponseDto>> intake(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePetRequestDto request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                petMapper.toDto(commandPort.intake(id, request, UUID.fromString(authentication.getName())))));
    }

    @PostMapping("/{id}/keep-pet")
    @Operation(summary = "Add the animal I rescued to my pets",
            description = "Only for the rescuer, after their report with outcome KEPT_BY_RESCUER was confirmed.")
    public ResponseEntity<ApiResponse<PetResponseDto>> keepPet(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePetRequestDto request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                petMapper.toDto(commandPort.keepPet(id, request, UUID.fromString(authentication.getName())))));
    }

    @GetMapping
    @Operation(summary = "List all rescue cases (paginated, with optional filters)",
            description = "Unified endpoint for listing rescue cases. status, priority, species and province (code or name) " +
                    "apply to every mode. Supports geo filtering with lat/lng/distance for nearby results " +
                    "or minLat/minLng/maxLat/maxLng for bounding box results. Use lat=X&lng=Y&distance=Z for nearby. " +
                    "Use minLat=X&minLng=Y&maxLat=Z&maxLng=W for bounding box.")
    public ResponseEntity<ApiResponse<PageResponse<RescueCaseSummaryResponseDto>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "reportedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) List<RescueCaseStatus> status,
            @RequestParam(required = false) List<RescuePriority> priority,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double distance,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double maxLng) {

        RescueCaseFilter filter = new RescueCaseFilter(status, priority, species, province);

        // Geo filter: bounding box
        if (minLat != null && minLng != null && maxLat != null && maxLng != null) {
            return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findWithinBoundingBox(minLat, minLng, maxLat, maxLng, filter, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)))));
        }

        // Geo filter: nearby
        if (lat != null && lng != null) {
            double dist = distance != null ? distance : 5000;
            return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findNearby(lat, lng, dist, filter, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)))));
        }

        // Standard list
        return ResponseEntity.ok(ApiResponse.ok(
            PageResponse.from(queryPort.findAll(search, filter, PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAP MARKER ENDPOINTS - Ultra-fast for real-time map rendering
    // Returns lightweight markers (no pagination - limited to 500 per request)
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/map/markers")
    @Operation(summary = "Get map markers in bounding box",
            description = "Ultra-lightweight endpoint for map rendering. Returns up to 500 markers with minimal data (~100 bytes each).")
    public ResponseEntity<ApiResponse<List<RescueMapMarkerDto>>> getMapMarkers(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) List<RescueCaseStatus> status,
            @RequestParam(required = false) List<RescuePriority> priority,
            @RequestParam(required = false) String species) {

        List<RescueMapMarkerDto> markers;
        boolean hasBbox = minLat != null && minLng != null && maxLat != null && maxLng != null;

        if (hasBbox) {
            if ((status == null || status.isEmpty()) && (priority == null || priority.isEmpty()) && species == null) {
                markers = queryPort.findMarkersInBounds(minLat, minLng, maxLat, maxLng);
            } else {
                markers = queryPort.findMarkersWithFilters(minLat, minLng, maxLat, maxLng, status, priority, species);
            }
        } else {
            // no bbox provided — fall back to global map feed limited to 500
            markers = queryPort.findMapMarkers(status, priority, species);
        }

        return ResponseEntity.ok(ApiResponse.ok(markers));
    }
    // ══════════════════════════════════════════════════════════════════════════
    // RESCUE COMPLETION FLOW ENDPOINTS
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/complete")
    @Operation(summary = "Submit rescue completion")
    public ResponseEntity<ApiResponse<RescueCaseCompletion>> complete(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRescueCompletionRequestDto cmd,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(commandPort.complete(id, cmd, userId)));
    }

    @GetMapping("completions/{id}")
    @Operation(summary = "Get rescue completion details")
    public ResponseEntity<ApiResponse<RescueCaseCompletionResponseDto>> getCompletionDetails(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.getCompletionDetails(id)));
    }

    @PostMapping("/completions/{id}/approve")
    @Operation(summary = "Approve rescue completion")
    public ResponseEntity<ApiResponse<RescueCaseCompletion>> approveCompletion(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok( commandPort.approveCompletion(id, userId)));
    }

    @PostMapping("/completions/{id}/confirm")
    @Operation(summary = "The reporter confirms another person's completion report")
    public ResponseEntity<ApiResponse<RescueCaseCompletion>> confirmCompletion(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(commandPort.confirmCompletion(id, UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/completions/{id}/dispute")
    @Operation(summary = "The reporter disagrees with a completion report", description = "An admin then decides.")
    public ResponseEntity<ApiResponse<RescueCaseCompletion>> disputeCompletion(
            @PathVariable UUID id,
            @Valid @RequestBody RescueCompletionDecisionRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                commandPort.disputeCompletion(id, request.getReason(), UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/completions/{id}/reject")
    @Operation(summary = "Reject a completion report", description = "The case can then receive another report.")
    public ResponseEntity<ApiResponse<RescueCaseCompletion>> rejectCompletion(
            @PathVariable UUID id,
            @Valid @RequestBody RescueCompletionDecisionRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                commandPort.rejectCompletion(id, request.getReason(), UUID.fromString(authentication.getName()))));
    }

    @GetMapping("/completions/review-queue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reports waiting for an admin: disputed, overdue, or with nobody else to confirm")
    public ResponseEntity<ApiResponse<PageResponse<RescueCaseCompletionResponseDto>>> getReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                queryPort.findCompletionReviewQueue(PageRequest.of(page, pageSize)))));
    }

    @GetMapping("/completions")
    @Operation(summary = "List all rescue completions (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<RescueCaseCompletionResponseDto>>> getAllCompletions(
            @RequestParam(defaultValue = "false") boolean isResolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
            PageResponse.from(queryPort.findAllCompletions(isResolved,PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)))));
    }

    /** Rescue case GET routes are public, so the caller may be anonymous. */
    private static UUID viewerId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return UUID.fromString(authentication.getName());
    }
}
