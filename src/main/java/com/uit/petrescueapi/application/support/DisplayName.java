package com.uit.petrescueapi.application.support;

/**
 * The name other people see for a user: their full name, or a neutral label when they never
 * gave one. Never the username: that is what people type to sign in, so it stays private.
 */
public final class DisplayName {

    /** Shown for a user who has not filled in a full name. */
    public static final String FALLBACK = "Thành viên PawHome";

    private DisplayName() {
    }

    public static String of(String fullName) {
        return fullName != null && !fullName.isBlank() ? fullName.trim() : FALLBACK;
    }
}
