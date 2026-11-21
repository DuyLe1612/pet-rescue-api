package com.uit.petrescueapi.application.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin-only request for creating a user account and assigning
 * an organization role in a single operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create organization-linked user account request")
public class CreateOrganizationAccountRequestDto {

    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(example = "rescue.owner")
    private String username;

    @NotBlank
    @Email
    @Schema(example = "owner@happypaws.org")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(example = "P@ssw0rd123")
    private String password;

    /** The name other people see on the platform. Required; the username stays private. */
    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    @Schema(example = "Nguyễn Thị Lan")
    private String fullName;

    @NotBlank
    @Schema(example = "OWNER", allowableValues = {"OWNER", "STAFF", "VET", "MEMBER"})
    private String organizationRole;

    @Schema(example = "MEMBER", description = "System role code (e.g. USER, MEMBER, ADMIN). Defaults to MEMBER when empty.")
    private String systemRole;
}
