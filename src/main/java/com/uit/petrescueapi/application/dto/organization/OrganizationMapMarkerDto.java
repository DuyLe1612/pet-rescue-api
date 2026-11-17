package com.uit.petrescueapi.application.dto.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * DTO for organization map markers.
 * Carries what a marker popup shows, so opening one needs no second request.
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
    private String organizationCode;
    private String phone;
    private String imageUrl;
    private String streetAddress;
    private String wardName;
    private String provinceName;
}