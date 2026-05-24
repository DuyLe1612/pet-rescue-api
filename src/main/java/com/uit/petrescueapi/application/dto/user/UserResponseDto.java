package com.uit.petrescueapi.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information")
public class UserResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(example = "U-0001")
    private String userCode;

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @Schema(example = "Paws Rescue Center")
    private String organizationName;

    @Schema(example = "OWNER", allowableValues = {"OWNER", "MANAGER", "MEMBER", "VOLUNTEER"})
    private String organizationRole;

    @Schema(example = "johndoe")
    private String username;

    @Schema(example = "john@example.com")
    private String email;

    @Schema(example = "John Doe")
    private String fullName;

    @Schema(example = "https://cdn.example.com/avatars/john.jpg")
    private String avatarUrl;

    @Schema(example = "+84912345678")
    private String phone;

    @Schema(example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private String gender;

    @Schema(example = "123 Nguyen Trai Street")
    private String streetAddress;

    @Schema(example = "00001")
    private String wardCode;

    @Schema(example = "Ward 1")
    private String wardName;

    @Schema(example = "79")
    private String provinceCode;

    @Schema(example = "Ho Chi Minh City")
    private String provinceName;

    @Schema(example = "ACTIVE", allowableValues = {"PENDING_VERIFICATION", "ACTIVE", "LOCKED", "INACTIVE", "BANNED"})
    private String status;

    @Schema(example = "true")
    private boolean emailVerified;

    private UserReputationResponseDto reputation;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
