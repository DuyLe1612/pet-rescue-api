package com.uit.petrescueapi.application.dto.analysis;

import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;

import java.util.List;

/**
 * Structured data extracted from a free-text post. Every field may be null except
 * {@code category}; unknown values are null rather than guessed.
 *
 * @param species canonical (English) pet species name from the taxonomy, e.g. "Dog"
 * @param urgency only meaningful when {@code needsRescue}
 */
public record PostAnalysisSuggestion(
        PostCategory category,
        String species,
        String locationText,
        String district,
        String province,
        boolean needsRescue,
        String rescueSummary,
        RescuePriority urgency,
        List<String> tags,
        Double categoryConfidence,
        Double speciesConfidence,
        Double locationConfidence) {

    public PostAnalysisSuggestion {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
