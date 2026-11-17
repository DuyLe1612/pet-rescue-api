package com.uit.petrescueapi.application.dto.followup;

import jakarta.validation.constraints.NotNull;

public record FollowupPreferenceDto(@NotNull Boolean enabled) {
}
