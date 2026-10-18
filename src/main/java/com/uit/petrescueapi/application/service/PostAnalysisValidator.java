package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Backend check of the model's structured output before it enters the business workflow
 * (DA2 §3: "Kết quả AI sau đó được backend kiểm tra"). Values outside the catalog are cleared,
 * never trusted; every correction is reported so the evaluation can count invalid outputs.
 */
@Component
public class PostAnalysisValidator {

    static final int MAX_TAGS = 10;

    public record Outcome(PostAnalysisSuggestion suggestion, List<String> errors, boolean fatal) {
    }

    public Outcome validate(PostAnalysisSuggestion s, Catalog catalog) {
        List<String> errors = new ArrayList<>();
        if (s == null || s.category() == null) {
            errors.add("category missing");
            return new Outcome(s, errors, true);
        }

        String species = canonicalSpecies(s.species(), catalog.speciesNames());
        Double speciesConfidence = s.speciesConfidence();
        if (s.species() != null && !s.species().isBlank() && species == null) {
            errors.add("unknown species: " + s.species());
            speciesConfidence = null;
        }

        PostAnalysisSuggestion clean = new PostAnalysisSuggestion(
                s.category(),
                species,
                blankToNull(s.locationText()),
                blankToNull(s.district()),
                blankToNull(s.province()),
                s.needsRescue(),
                s.needsRescue() ? blankToNull(s.rescueSummary()) : null,
                s.needsRescue() ? s.urgency() : null,
                knownTags(s.tags(), catalog.tagCodes()),
                clamp("category confidence", s.categoryConfidence(), errors),
                species == null ? null : clamp("species confidence", speciesConfidence, errors),
                clamp("location confidence", s.locationConfidence(), errors));
        return new Outcome(clean, errors, false);
    }

    private static String canonicalSpecies(String value, List<String> names) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return names.stream().filter(n -> n.equalsIgnoreCase(value.trim())).findFirst().orElse(null);
    }

    private static List<String> knownTags(List<String> tags, List<String> codes) {
        Set<String> allowed = Set.copyOf(codes);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) {
            String t = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
            if (allowed.contains(t) && result.size() < MAX_TAGS) {
                result.add(t);
            }
        }
        return List.copyOf(result);
    }

    private static Double clamp(String field, Double value, List<String> errors) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 1 || value.isNaN()) {
            errors.add(field + " out of [0,1]: " + value);
            return value.isNaN() ? null : Math.max(0, Math.min(1, value));
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
