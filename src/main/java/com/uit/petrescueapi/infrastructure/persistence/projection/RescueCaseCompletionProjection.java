package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RescueCaseCompletionProjection {

    UUID getCompletionId();

    UUID getCaseId();

    LocalDateTime getRescuedAt();

    String getRescueNote();

    String getLocationNote();

    UUID getVerifiedBy();

    String getVerifiedByName();

    LocalDateTime getVerifiedAt();
}
