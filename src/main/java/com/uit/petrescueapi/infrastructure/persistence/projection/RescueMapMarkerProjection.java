package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ultra-lightweight projection for map markers.
 * Minimal columns for fast query and rendering.
 * No JOINs needed - all data from rescue_cases table.
 */
public interface RescueMapMarkerProjection {

    UUID getCaseId();
    String getCaseCode();
    Double getLatitude();
    Double getLongitude();
    String getPriority();
    String getStatus();
    String getSpecies();
    String getCaseType();
    String getHoldingStatus();
    Integer getLocationBlurMeters();
    Boolean getNeedsHelp();
    LocalDateTime getReportedAt();
}
