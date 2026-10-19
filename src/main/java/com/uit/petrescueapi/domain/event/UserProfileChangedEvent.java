package com.uit.petrescueapi.domain.event;

import java.util.UUID;

/** A user changed a field that MongoDB documents snapshot (username, full name, avatar). */
public record UserProfileChangedEvent(UUID userId) {
}
