package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.admin.AssignOrgRoleRequestDto;
import com.uit.petrescueapi.application.dto.admin.CreateAdminAccountRequestDto;
import com.uit.petrescueapi.application.dto.admin.CreateOrganizationAccountRequestDto;
import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationMemberResponseDto;
import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.domain.entity.OrganizationMember;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.mapper.PetWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Administrative endpoints — ADMIN role required for all operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {

    private final UserDomainService userDomainService;
    private final OrganizationDomainService organizationDomainService;
    private final PetCommandPort petCommandPort;
    private final PetWebMapper petWebMapper;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/accounts")
    @Operation(summary = "Create a fully custom account (default ACTIVE)")
    public ResponseEntity<ApiResponse<UserResponseDto>> createAccount(
            @Valid @RequestBody CreateAdminAccountRequestDto cmd) {

        String systemRole = (cmd.getSystemRole() == null || cmd.getSystemRole().isBlank())
                ? "USER"
                : cmd.getSystemRole();

        String hashedPassword = passwordEncoder.encode(cmd.getPassword());

        User user = userDomainService.createCustomActiveUser(
                cmd.getUsername(),
                cmd.getEmail(),
                hashedPassword,
                systemRole,
                cmd.getFullName(),
                cmd.getAvatarUrl(),
                cmd.getPhone(),
                cmd.getGender(),
                cmd.getStreetAddress(),
                cmd.getWardCode(),
                cmd.getWardName(),
                cmd.getProvinceCode(),
                cmd.getProvinceName()
        );

        UserResponseDto dto = UserResponseDto.builder()
                .userId(user.getId())
                .userCode(user.getUserCode())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .gender(user.getGender())
                .streetAddress(user.getStreetAddress())
                .wardName(user.getWardName())
                .provinceName(user.getProvinceName())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getCode()).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PostMapping("/organizations/{organizationId}/accounts")
    @Operation(summary = "Create a new user account and assign an organization role")
    public ResponseEntity<ApiResponse<UserResponseDto>> createOrganizationAccount(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateOrganizationAccountRequestDto cmd) {

        String systemRole = (cmd.getSystemRole() == null || cmd.getSystemRole().isBlank())
                ? "MEMBER"
                : cmd.getSystemRole();

        String hashedPassword = passwordEncoder.encode(cmd.getPassword());

        User user = userDomainService.createUser(
                cmd.getUsername(),
                cmd.getEmail(),
                hashedPassword,
                systemRole
        );

        organizationDomainService.addMember(organizationId, user.getId(), cmd.getOrganizationRole());

        UserResponseDto dto = UserResponseDto.builder()
                .userId(user.getId())
                .userCode(user.getUserCode())
                .organizationId(organizationId)
                .organizationName(null)
                .organizationRole(cmd.getOrganizationRole())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getCode()).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PostMapping("/organizations/{organizationId}/members")
    @Operation(summary = "Assign an organization role to an existing user (target must not be an admin account)")
    public ResponseEntity<ApiResponse<OrganizationMemberResponseDto>> assignOrgRole(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AssignOrgRoleRequestDto cmd) {

        User targetUser = userDomainService.findById(cmd.getUserId());
        if (targetUser.hasRole("ADMIN")) {
            throw new BusinessException("Cannot assign an organization role to an admin account", "FORBIDDEN_TARGET");
        }

        OrganizationMember member = organizationDomainService.addMember(
                organizationId, cmd.getUserId(), cmd.getOrganizationRole());

        OrganizationMemberResponseDto dto = OrganizationMemberResponseDto.builder()
                .organizationId(member.getOrganizationId())
                .organizationName(null)
                .userId(member.getUserId())
                .username(targetUser.getUsername())
                .role(member.getRole())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

        @PostMapping("/organizations/{organizationId}/pets")
        @Operation(summary = "Create pet for an organization, optionally assigning a caretaker user")
    public ResponseEntity<ApiResponse<PetResponseDto>> createPetForUserInOrganization(
            @PathVariable UUID organizationId,
                        @RequestParam(required = false) UUID userId,
            @Valid @RequestBody CreatePetRequestDto cmd) {

        Pet created = petCommandPort.createForUserInOrganization(cmd, organizationId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(petWebMapper.toDto(created)));
    }

    @PatchMapping("/users/{userId}/lock")
    @Operation(summary = "Lock user account")
    public ResponseEntity<ApiResponse<UserResponseDto>> lockUser(@PathVariable UUID userId) {
        User user = userDomainService.lockAccount(userId);
        UserResponseDto dto = UserResponseDto.builder()
                .userId(user.getId())
                .userCode(user.getUserCode())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getCode()).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PatchMapping("/users/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    public ResponseEntity<ApiResponse<UserResponseDto>> unlockUser(@PathVariable UUID userId) {
        User user = userDomainService.unlockAccount(userId);
        UserResponseDto dto = UserResponseDto.builder()
                .userId(user.getId())
                .userCode(user.getUserCode())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getCode()).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}
