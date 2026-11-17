package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.domain.entity.RescueCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The parts of triage that need no model: what text the model reads, and what a rescuer would
 * want that the report lacks. Plain rules, so they work when the AI is off or fails.
 */
public final class TriageRules {

    public static final String PHOTO = "PHOTO";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String SPECIES = "SPECIES";
    public static final String LOCATION = "LOCATION";

    /** A description shorter than this says almost nothing about the animal's condition. */
    static final int MIN_DESCRIPTION = 10;

    private TriageRules() {
    }

    /**
     * The report as plain labelled lines. The reporter's own priority is left out on purpose: the
     * model judges from the facts, so its answer can be compared with the reporter's.
     */
    public static String textFor(RescueCase rescueCase) {
        String place = Stream.of(rescueCase.getLocationText(), rescueCase.getWardName(), rescueCase.getProvinceName())
                .filter(TriageRules::hasText).map(String::trim).distinct().collect(Collectors.joining(", "));
        return Stream.of(
                        line("Loài", rescueCase.getSpecies()),
                        line("Màu", rescueCase.getColor()),
                        line("Kích cỡ", rescueCase.getSize()),
                        line("Địa điểm", place),
                        line("Mô tả", rescueCase.getDescription()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    /** Facts missing from the report, in a stable order. */
    public static List<String> missingFields(RescueCase rescueCase, int photos) {
        List<String> missing = new ArrayList<>();
        if (photos == 0) {
            missing.add(PHOTO);
        }
        if (!hasText(rescueCase.getDescription()) || rescueCase.getDescription().trim().length() < MIN_DESCRIPTION) {
            missing.add(DESCRIPTION);
        }
        if (!hasText(rescueCase.getSpecies())) {
            missing.add(SPECIES);
        }
        if (!hasText(rescueCase.getLocationText()) && !hasText(rescueCase.getWardName())) {
            missing.add(LOCATION);
        }
        return missing;
    }

    private static String line(String label, String value) {
        return hasText(value) ? label + ": " + value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
