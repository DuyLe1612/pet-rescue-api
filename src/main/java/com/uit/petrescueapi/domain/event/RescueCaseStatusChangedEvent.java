package com.uit.petrescueapi.domain.event;

import java.util.UUID;

/** A rescue case changed status; posts that reference it show the status. */
public record RescueCaseStatusChangedEvent(UUID caseId, String status) {
}
