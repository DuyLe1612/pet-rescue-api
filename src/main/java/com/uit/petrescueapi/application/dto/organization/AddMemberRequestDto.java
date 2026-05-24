package com.uit.petrescueapi.application.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for adding a member to an organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequestDto {

    @NotNull
    private UUID userId;
    @NotBlank
    @Pattern(regexp = "^(OWNER|STAFF|VET|MEMBER)$")
    private String role;  // STAFF | VET (org endpoint); OWNER | STAFF | VET (admin endpoint)
}
