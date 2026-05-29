package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for organization member.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization member")
public class OrganizationMemberResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @Schema(example = "Happy Paws Shelter")
    private String organizationName;

    @Schema(example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userId;

    @Schema(example = "johndoe")
    private String username;

    @Schema(example = "STAFF", allowableValues = {"OWNER", "STAFF", "MEMBER", "VET"})
    private String role;

    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;

    private LocalDateTime joinedAt;
}
