package com.uit.petrescueapi.application.dto.followup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Answers sent from the link in the email, without signing in. */
public record FollowupRespondRequestDto(@NotBlank String token, @NotNull @Valid FollowupAnswersRequestDto answers) {
}
