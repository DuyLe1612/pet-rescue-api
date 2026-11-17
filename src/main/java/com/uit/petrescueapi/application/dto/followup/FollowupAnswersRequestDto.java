package com.uit.petrescueapi.application.dto.followup;

import com.uit.petrescueapi.domain.valueobject.FollowupAnswers;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The five questions, plus an optional note. If the pet is gone the others may be left out. */
public record FollowupAnswersRequestDto(
        @NotNull Boolean stillHasPet,
        FollowupAnswers.Wellbeing wellbeing,
        FollowupAnswers.Fear fear,
        FollowupAnswers.Incident incident,
        FollowupAnswers.Support support,
        @Size(max = 500) String note) {

    public FollowupAnswers toDomain() {
        return new FollowupAnswers(stillHasPet, wellbeing, fear, incident, support, note);
    }
}
