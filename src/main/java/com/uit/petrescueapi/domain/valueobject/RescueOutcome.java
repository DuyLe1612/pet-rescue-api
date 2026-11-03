package com.uit.petrescueapi.domain.valueobject;

/** What happened to the animal, as stated in a completion report. */
public enum RescueOutcome {
    HANDED_TO_ORG(false),
    TEMP_HOLDING(false),
    KEPT_BY_RESCUER(true),
    RELEASED(true),
    DECEASED(true);

    private final boolean closesCase;

    RescueOutcome(boolean closesCase) {
        this.closesCase = closesCase;
    }

    /** True when nothing is left to do after approval, so the case goes straight to CLOSED. */
    public boolean closesCase() {
        return closesCase;
    }
}
