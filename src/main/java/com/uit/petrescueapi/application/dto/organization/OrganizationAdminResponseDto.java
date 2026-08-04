package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin response DTO for organization (shelter or vet center).
 * Contains all fields including organizationCode, description, contact details,
 * and audit fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization for Admin (includes all internal fields)")
public class OrganizationAdminResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @Schema(example = "O-1001")
    private String organizationCode;

    @Schema(example = "Happy Paws Shelter")
    private String name;

    @Schema(example = "A shelter dedicated to rescuing and rehoming abandoned pets in Ho Chi Minh City.")
    private String description;

    @Schema(example = "SHELTER", allowableValues = {"SHELTER", "VET_CENTER"})
    private String type;

    @Schema(example = "123 Nguyen Trai")
    private String streetAddress;

    @Schema(example = "Phuong 1")
    private String wardName;

    @Schema(example = "Ho Chi Minh")
    private String provinceName;

    @Schema(example = "+84-28-1234-5678")
    private String phone;

    @Schema(example = "contact@shelter.vn")
    private String email;

    @Schema(example = "https://cdn.example.com/organizations/org-cover.jpg")
    private String imageUrl;

    @Schema(example = "https://facebook.com/happypaws")
    private String officialLink;

    @Schema(example = "10.762622")
    private Double latitude;

    @Schema(example = "106.660172")
    private Double longitude;

    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "PENDING"})
    private String status;

    @Schema(description = "User who requested this organization (for pending orgs)")
    private UUID requestedByUserId;

    private String requestedByUsername;

    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
