package com.uit.petrescueapi.domain.valueobject;

/** Who the description of a pet comes from; it sets how sure the match can be. */
public enum TraitSource {
    /** Lives with the pet every day. */
    FOSTER(1.0),
    /** Saw the pet directly at the shelter. */
    STAFF(0.7),
    /** One person's account, not observed by the organization. */
    PREVIOUS_OWNER(0.4),
    OWNER(0.4);

    private final double certainty;

    TraitSource(double certainty) {
        this.certainty = certainty;
    }

    /** Starting assumption, not measured: see agent/04-design-matching.md. */
    public double certainty() {
        return certainty;
    }
}
