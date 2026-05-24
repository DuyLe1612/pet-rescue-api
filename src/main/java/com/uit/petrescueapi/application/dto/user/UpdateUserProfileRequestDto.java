package com.uit.petrescueapi.application.dto.user;

import com.uit.petrescueapi.validation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating the current user's profile.
 * Email is intentionally excluded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequestDto {

    @Schema(example = "johndoe")
    @Size(max = 100)
    private String username;

    @Schema(example = "John Doe")
    @Size(max = 255)
    private String fullName;

    @Schema(example = "https://cdn.example.com/avatars/john.jpg")
    @Size(max = 500)
    private String avatarUrl;

    @Schema(example = "+84912345678")
    @Phone
    private String phone;

    @Schema(example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    @Size(max = 20)
    private String gender;

    @Schema(example = "123 Nguyen Trai Street")
    @Size(max = 255)
    private String streetAddress;

    @Schema(example = "00001")
    @Size(max = 50)
    private String wardCode;

    @Schema(example = "Ward 1")
    @Size(max = 255)
    private String wardName;

    @Schema(example = "79")
    @Size(max = 50)
    private String provinceCode;

    @Schema(example = "Ho Chi Minh City")
    @Size(max = 255)
    private String provinceName;
}