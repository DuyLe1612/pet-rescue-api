package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank
    @Size(max = 255)
    private String name;

    @Schema(example = "A shelter dedicated to rescuing and rehoming abandoned pets in Ho Chi Minh City.")
    @Size(max = 2000)
    private String description;

    @Schema(example = "SHELTER", allowableValues = {"SHELTER", "VET_CENTER"})
    @NotBlank
    @Size(max = 50)
    private String type;

    @Schema(example = "123 Nguyen Trai")
    @NotBlank
    @Size(max = 255)
    private String streetAddress;

    @Schema(example = "00001", description = "Ward code")
    @NotBlank
    @Size(max = 50)
    private String wardCode;

    @Schema(example = "Phường 1", description = "Ward name")
    @NotBlank
    @Size(max = 255)
    private String wardName;

    @Schema(example = "79", description = "Province code")
    @NotBlank
    @Size(max = 50)
    private String provinceCode;

    @Schema(example = "Hồ Chí Minh", description = "Province name")
    @NotBlank
    @Size(max = 255)
    private String provinceName;

    @Schema(example = "+84-28-1234-5678")
    @Phone
    private String phone;

    @Schema(example = "contact@shelter.vn")
    @Email(message = "Email must be valid")
    @NotBlank
    private String email;

    @Schema(example = "https://cdn.example.com/organizations/org-cover.jpg")
    @Size(max = 500)
    private String imageUrl;

    @Schema(example = "https://facebook.com/happypaws")
    @Size(max = 500)
    private String officialLink;

    @Schema(example = "10.762622")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @Schema(example = "106.660172")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
}
