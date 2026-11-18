package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RescueQueueItemProjection {
    UUID getCaseId();
    String getCaseCode();
    String getSpecies();
    String getPriority();
    String getStatus();
    String getDescription();
    String getLocationText();
    String getFirstImageUrl();
    String getProvinceName();
    String getWardName();
    Double getLatitude();
    Double getLongitude();
    LocalDateTime getReportedAt();
    boolean getNeedsHelp();
    boolean getAwaitingHandover();
    long getActiveClaimCount();
    /** Only from a finished triage. */
    String getAiUrgency();
    String getAiSummary();
    /** PENDING, ANALYZED or FAILED; null when the case was never triaged. */
    String getTriageStatus();
    /** Comma separated, only from a finished triage. */
    String getMissingFields();
}
