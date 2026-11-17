package com.uit.petrescueapi.application.dto.followup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** One piece of follow-up work for the organization. */
public record FollowupAlertDto(UUID alertId, UUID applicationId, UUID petId, String petName, String severity,
                               List<String> reasons, String status, UUID assignedTo, LocalDateTime createdAt,
                               LocalDateTime firstActionAt, LocalDateTime resolvedAt, String resolution, String note) {
}
