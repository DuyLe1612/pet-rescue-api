package com.uit.petrescueapi.application.dto.followup;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** One milestone on the timeline of an adoption. */
public record FollowupDto(UUID followupId, String milestone, String status, LocalDateTime dueAt, LocalDateTime sentAt,
                          LocalDateTime respondedAt, LocalDateTime expiresAt, String severity, Boolean stillHasPet,
                          Map<String, Object> answers) {
}
