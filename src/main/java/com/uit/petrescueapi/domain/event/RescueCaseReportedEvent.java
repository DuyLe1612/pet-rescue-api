package com.uit.petrescueapi.domain.event;

import java.util.UUID;

/** A rescue case was reported and saved; the AI triage may now label it. Nothing waits for it. */
public record RescueCaseReportedEvent(UUID caseId) {
}
