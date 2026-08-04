package com.uit.petrescueapi.application.dto.auth;

import com.uit.petrescueapi.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request")
public class LoginRequestDto {

    @NotBlank(message = "Email or username is required")
    @Schema(example = "john@example.com", description = "Email address (or username for login)")
    private String emailOrUsername;

    @NotBlank(message = "Password is required")
    @Password(message = "Password must be at least 8 characters and contain uppercase, lowercase, digit ")
    @Schema(example = "P@ssw0rd123", description = "User password (min 8 chars, must contain uppercase, lowercase, digit)")
    private String password;
}
