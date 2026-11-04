package com.uit.petrescueapi.domain.event;

import java.util.UUID;

/** A person took a case; used to open the chat with the reporter after commit. */
public record RescueCaseClaimedEvent(UUID caseId, UUID claimerId, UUID reporterId) {
}
