package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RescueCaseCompletionProjection {

    UUID getCompletionId();

    UUID getCaseId();
    String getCaseCode();

    LocalDateTime getRescuedAt();

    String getRescueNote();

    String getLocationNote();

    UUID getVerifiedBy();

    String getVerifiedByName();

    LocalDateTime getVerifiedAt();
    UUID getSubmittedBy();
    String getSubmittedByName();
    String getOutcome();
    String getStatus();
    String getRejectReason();
}
