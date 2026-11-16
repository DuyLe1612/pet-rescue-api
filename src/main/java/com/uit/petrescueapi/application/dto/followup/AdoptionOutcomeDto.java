package com.uit.petrescueapi.application.dto.followup;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdoptionOutcomeDto(UUID applicationId, UUID petId, String outcomeType, String reasonCode,
                                 LocalDateTime occurredAt, Integer daysSinceCompleted, String note) {
}
