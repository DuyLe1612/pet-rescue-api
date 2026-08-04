package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Minimal organization DTO with only id and name.
 * Used for lightweight references in other DTOs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Minimal organization reference")
public class OrganizationMinimalDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @Schema(example = "Happy Paws Shelter")
    private String name;

    @Schema(example = "https://happypaws.example")
    private String url;
}
