package com.uit.petrescueapi.domain.valueobject;

/** The five weighted criteria. Weights come from the project proposal and add up to 100. */
public enum MatchCriterion {
    LIFESTYLE(25), HOUSING(20), FAMILY(20), EXISTING_PETS(20), READINESS(15);

    private final int weight;

    MatchCriterion(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
