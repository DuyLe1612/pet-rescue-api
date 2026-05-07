package com.uit.petrescueapi.application.dto.auth;

import com.uit.petrescueapi.validation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for user registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registration request")
public class RegisterRequestDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(example = "johndoe")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "john@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(example = "P@ssw0rd123")
    private String password;

    @Schema(example = "John Doe")
    private String fullName;

    @Schema(example = "+84912345678")
    @Phone
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
}
