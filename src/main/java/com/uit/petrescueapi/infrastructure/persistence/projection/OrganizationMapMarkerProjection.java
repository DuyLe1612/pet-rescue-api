package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

public interface OrganizationMapMarkerProjection {
    UUID getOrganizationId();
    String getOrganizationCode();
    String getName();
    String getType();
    String getStatus();
    Double getLatitude();
    Double getLongitude();
    String getPhone();
    String getImageUrl();
    String getWardName();
    String getProvinceName();
}