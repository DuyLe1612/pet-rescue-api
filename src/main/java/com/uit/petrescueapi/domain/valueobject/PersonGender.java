package com.uit.petrescueapi.domain.valueobject;

/**
 * A person's gender as the account holder states it. Stored by name in {@code users.gender}.
 * Pets use {@link Gender}, where the third value means "not known yet".
 */
public enum PersonGender {
    MALE,
    FEMALE,
    OTHER
}
