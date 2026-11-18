package com.uit.petrescueapi.domain.valueobject;

public enum CaseMatchStatus {
    PENDING,
    /** The owner said it is their pet. */
    CONFIRMED,
    /** The owner said it is not. */
    REJECTED,
    /** Left over when the lost case closed another way. */
    DISMISSED
}
