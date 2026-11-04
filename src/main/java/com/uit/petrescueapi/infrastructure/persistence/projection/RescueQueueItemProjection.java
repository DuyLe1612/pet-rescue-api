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
    String getProvinceName();
    String getWardName();
    Double getLatitude();
    Double getLongitude();
    LocalDateTime getReportedAt();
    boolean getNeedsHelp();
    boolean getAwaitingHandover();
    long getActiveClaimCount();
}
