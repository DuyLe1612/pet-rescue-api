package com.uit.petrescueapi.domain.valueobject;

/** How much of the result rests on recorded facts about the pet. */
public enum MatchCertainty {
    HIGH, MEDIUM, LOW, NONE;

    public static MatchCertainty of(double certainty) {
        if (certainty >= 0.85) {
            return HIGH;
        }
        if (certainty >= 0.55) {
            return MEDIUM;
        }
        return certainty > 0 ? LOW : NONE;
    }
}
