package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface projection for organization detail view.
 * Used by {@code OrganizationQueryJpaRepository.findDetailById()}.
 */
public interface OrganizationDetailProjection {
    UUID getOrganizationId();
    String getOrganizationCode();
    String getName();
    String getDescription();
    String getType();
    String getStreetAddress();
    String getWardCode();
    String getWardName();
    String getProvinceCode();
    String getProvinceName();
    String getPhone();
    String getEmail();
    String getImageUrl();
    String getOfficialLink();
    Double getLatitude();
    Double getLongitude();
    String getStatus();
    UUID getRequestedByUserId();
    String getRequestedByUsername();
    UUID getCreatedBy();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
