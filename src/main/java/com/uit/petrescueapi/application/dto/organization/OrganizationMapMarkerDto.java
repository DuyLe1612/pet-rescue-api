package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * DTO for organization map markers.
 * Optimized: excludes unused fields (organizationCode, phone, imageUrl).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization map marker")
public class OrganizationMapMarkerDto {
    private UUID organizationId;
    private String name;
    private String type;
    private String status;
    private Double latitude;
    private Double longitude;
    private String wardName;
    private String provinceName;
}