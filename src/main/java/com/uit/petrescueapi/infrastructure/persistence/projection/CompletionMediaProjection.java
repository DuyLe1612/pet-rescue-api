package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.util.UUID;

public interface CompletionMediaProjection {
    UUID getCompletionId();
    String getPublicId();
}
