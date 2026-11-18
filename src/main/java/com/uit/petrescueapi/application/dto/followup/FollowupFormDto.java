package com.uit.petrescueapi.application.dto.followup;

import java.util.List;

/** What the public page shows: the pet's name and the questions. Nothing about the person. */
public record FollowupFormDto(String petName, String milestone, String intro, List<Question> questions) {

    public record Question(String key, String text, List<Option> options) {
    }

    public record Option(String value, String label) {
    }
}
