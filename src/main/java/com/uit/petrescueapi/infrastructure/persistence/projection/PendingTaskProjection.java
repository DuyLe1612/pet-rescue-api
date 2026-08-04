package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;

public interface PendingTaskProjection {
    String getTaskId();
    String getType();
    String getTitle();
    String getReferenceId();
    String getStatus();
    String getPriority();
    LocalDateTime getCreatedAt();
    String getActionUrl();
}
