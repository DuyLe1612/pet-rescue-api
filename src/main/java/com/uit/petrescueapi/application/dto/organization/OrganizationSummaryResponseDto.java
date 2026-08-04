package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Lightweight response DTO for organization list views.
 * Optimized: excludes unused fields (organizationCode, streetAddress, phone, email, imageUrl).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization summary for list views")
public class OrganizationSummaryResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @Schema(example = "Happy Paws Shelter")
    private String name;

    @Schema(example = "SHELTER", allowableValues = {"SHELTER", "VET_CENTER"})
    private String type;

    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "PENDING"})
    private String status;

    @Schema(example = "Yên Láng")
    private String wardName;

    @Schema(example = "Hà Nội")
    private String provinceName;
}
