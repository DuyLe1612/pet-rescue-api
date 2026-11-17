package com.uit.petrescueapi.domain.valueobject;

/** Why someone in the community thinks an open case needs no more help. */
public enum RescueCaseFlagReason {
    /** The animal or the situation described does not exist. */
    FALSE_REPORT,
    /** Someone already took care of the animal. */
    ALREADY_HANDLED,
    /** The same animal is reported in another case. */
    DUPLICATE
}
