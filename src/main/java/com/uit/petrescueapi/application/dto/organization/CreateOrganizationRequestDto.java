package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.Email;
import com.uit.petrescueapi.validation.Phone;

/**
 * Request DTO for creating or updating an organization.
 * Includes both codes and names for location fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequestDto {

    @Schema(example = "Happy Paws Shelter")
    private String name;

    @Schema(example = "A shelter dedicated to rescuing and rehoming abandoned pets in Ho Chi Minh City.")
    private String description;

    @Schema(example = "SHELTER", allowableValues = {"SHELTER", "VET_CENTER"})
    private String type;

    @Schema(example = "123 Nguyen Trai")
    private String streetAddress;

    @Schema(example = "00001", description = "Ward code")
    private String wardCode;

    @Schema(example = "Phường 1", description = "Ward name")
    private String wardName;

    @Schema(example = "79", description = "Province code")
    private String provinceCode;

    @Schema(example = "Hồ Chí Minh", description = "Province name")
    private String provinceName;

    @Schema(example = "+84-28-1234-5678")
    @Phone
    private String phone;

    @Schema(example = "contact@shelter.vn")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(example = "https://cdn.example.com/organizations/org-cover.jpg")
    private String imageUrl;

    @Schema(example = "https://facebook.com/happypaws")
    private String officialLink;

    @Schema(example = "10.762622")
    private Double latitude;

    @Schema(example = "106.660172")
    private Double longitude;
}
