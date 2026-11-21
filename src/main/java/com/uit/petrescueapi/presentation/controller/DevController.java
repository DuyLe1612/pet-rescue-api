package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Development-only endpoints.
 * Activate with the `development` Spring profile.
 *
 * <p>Uses domain service for proper transaction boundaries and business rules.</p>
 */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevController {

    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;

    public static class CreateAdminRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        public String username;

        @NotBlank
        @Email
        public String email;

        @NotBlank
        @Size(min = 6, max = 128)
        public String password;

        @NotBlank
        @Size(max = 255)
        public String fullName;

        // optional role code, default ADMIN
        public String role = "ADMIN";
    }

    @PostMapping("/create-account")
    public ResponseEntity<ApiResponse<UserResponseDto>> createAccount(@Valid @RequestBody CreateAdminRequest req) {
        String hashedPassword = passwordEncoder.encode(req.password);

        // Use domain service — encapsulates validation and transaction
        User saved = userDomainService.createUser(req.username, req.email, hashedPassword, req.fullName, req.role);

        UserResponseDto dto = UserResponseDto.builder()
                .userId(saved.getId())
                .organizationId(null)          // Not applicable for dev-created accounts
                .organizationName(null)
                .organizationRole(null)
                .username(saved.getUsername())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .phone(saved.getPhone())
                .gender(saved.getGender())
                .streetAddress(saved.getStreetAddress())
                .wardName(saved.getWardName())
                .provinceName(saved.getProvinceName())
                .status(saved.getStatus().name())
                .emailVerified(saved.isEmailVerified())
                .roles(saved.getRoles().stream().map(r -> r.getCode()).toList())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();

        return ResponseEntity.status(201).body(ApiResponse.created(dto));
    }
}
