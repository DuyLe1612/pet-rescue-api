package com.uit.petrescueapi.application.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin-only request to create a fully custom account")
public class CreateAdminAccountRequestDto {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @Schema(example = "USER", description = "System role code. Defaults to USER when empty.")
    private String systemRole;

    private String fullName;
    private String avatarUrl;
    @com.uit.petrescueapi.validation.Phone
    private String phone;
    private String gender;
    private String streetAddress;
    private String wardCode;
    private String wardName;
    private String provinceCode;
    private String provinceName;
}

