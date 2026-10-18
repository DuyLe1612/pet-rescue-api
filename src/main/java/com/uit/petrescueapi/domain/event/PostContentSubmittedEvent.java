package com.uit.petrescueapi.domain.event;

import java.util.UUID;

/** A post was created or its text changed; it may need (re-)analysis by the AI. */
public record PostContentSubmittedEvent(UUID postId) {
}
