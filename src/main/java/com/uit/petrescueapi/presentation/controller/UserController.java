package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.user.UserReputationResponseDto;
import com.uit.petrescueapi.application.dto.user.UpdateUserProfileRequestDto;
import com.uit.petrescueapi.application.dto.user.UserPublicSearchDto;
import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.application.dto.user.UserSummaryResponseDto;
import com.uit.petrescueapi.application.port.command.UserCommandPort;
import com.uit.petrescueapi.application.port.query.UserQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.UserWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Users", description = "User management")
public class UserController {

    private final UserCommandPort commandPort;
    private final UserQueryPort queryPort;
    private final com.uit.petrescueapi.application.port.query.RescueCaseQueryPort rescueQueryPort;
    private final com.uit.petrescueapi.application.port.query.PetQueryPort petQueryPort;
    private final UserWebMapper mapper;

    @PutMapping("/me/profile")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequestDto request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.updateProfile(userId, request))));
    }

    @PatchMapping("/me/profile")
    @Operation(summary = "Update current user profile partially")
    public ResponseEntity<ApiResponse<UserResponseDto>> patchProfile(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String avatarUrl,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UpdateUserProfileRequestDto request = UpdateUserProfileRequestDto.builder()
                .username(username)
                .avatarUrl(avatarUrl)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.updateProfile(userId, request))));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(userId)));
    }

    @GetMapping("/me/rescue-claims")
    @Operation(summary = "Rescue cases I am working on")
    public ResponseEntity<ApiResponse<java.util.List<com.uit.petrescueapi.application.dto.rescue.RescueClaimResponseDto>>> getMyRescueClaims(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(rescueQueryPort.findMyActiveClaims(UUID.fromString(authentication.getName()))));
    }

    @GetMapping("/me/rescue-cases")
    @Operation(summary = "Rescue cases I reported",
            description = "Newest first. status narrows it, e.g. status=REPORTED&status=IN_PROGRESS for the ones still open.")
    public ResponseEntity<ApiResponse<PageResponse<com.uit.petrescueapi.application.dto.rescue.RescueCaseSummaryResponseDto>>> getMyRescueCases(
            @RequestParam(required = false) java.util.List<com.uit.petrescueapi.domain.valueobject.RescueCaseStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(rescueQueryPort.findAll(null,
                com.uit.petrescueapi.application.dto.rescue.RescueCaseFilter.reportedBy(userId, status),
                PageableRequestFactory.ofNative(page, pageSize, "reportedAt", "desc")))));
    }

    @GetMapping("/me/pets")
    @Operation(summary = "Pets I own",
            description = "Every pet whose current owner is the caller: self-registered, adopted, or kept after a rescue.")
    public ResponseEntity<ApiResponse<PageResponse<com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto>>> getMyPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(petQueryPort.findAllWithFilters(
                null, null, null, null, null, userId, null, null,
                PageableRequestFactory.ofNative(page, pageSize, sortBy, sortOrder)))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID",
            description = "Email, phone and street address are only returned to the user themself and to admins.")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findByIdForViewer(id, UUID.fromString(authentication.getName()))));
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(search, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users for friend discovery")
    public ResponseEntity<ApiResponse<PageResponse<UserPublicSearchDto>>> searchPublicUsers(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<UserPublicSearchDto> result;
        if (search == null || search.isBlank()) {
            result = Page.empty(PageRequest.of(page, pageSize));
        } else {
            result = queryPort.searchPublicUsers(search, PageRequest.of(page, pageSize));
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/{id}/reputation")
    @Operation(summary = "Get user reputation")
    public ResponseEntity<ApiResponse<UserReputationResponseDto>> getReputation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.getReputation(id)));
    }
}
