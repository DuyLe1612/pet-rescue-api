package com.uit.petrescueapi.domain.valueobject;

/** Matching only tells dogs and cats apart; every other species shares the neutral rules. */
public enum SpeciesKind {
    DOG, CAT, OTHER;

    /** Accepts the English canonical name or the Vietnamese label of a species. */
    public static SpeciesKind fromName(String speciesName) {
        if (speciesName == null) {
            return OTHER;
        }
        String name = speciesName.trim().toLowerCase(java.util.Locale.ROOT);
        if (name.equals("dog") || name.equals("chó")) {
            return DOG;
        }
        if (name.equals("cat") || name.equals("mèo")) {
            return CAT;
        }
        return OTHER;
    }
}
