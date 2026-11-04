package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Response DTO for organization (shelter or vet center).
 * Carries every field the edit form sends back in {@link CreateOrganizationRequestDto}.
 * Excludes organizationCode, requestedByUserId, requestedByUsername, createdBy,
 * createdAt, updatedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization (shelter or vet center)")
public class OrganizationResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @Schema(example = "Happy Paws Shelter")
    private String name;

    @Schema(example = "SHELTER", allowableValues = {"SHELTER", "VET_CENTER"})
    private String type;

    @Schema(example = "A shelter dedicated to rescuing and rehoming abandoned pets.")
    private String description;

    @Schema(example = "123 Nguyen Trai")
    private String streetAddress;

    @Schema(example = "00001")
    private String wardCode;

    @Schema(example = "Phuong 1")
    private String wardName;

    @Schema(example = "79")
    private String provinceCode;

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
}
