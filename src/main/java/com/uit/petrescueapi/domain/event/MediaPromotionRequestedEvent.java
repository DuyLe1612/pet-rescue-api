package com.uit.petrescueapi.domain.event;

import java.util.UUID;

/** A TEMP upload now belongs to something that was saved, and should move to permanent storage. */
public record MediaPromotionRequestedEvent(UUID mediaId, String targetFolder) {
}
