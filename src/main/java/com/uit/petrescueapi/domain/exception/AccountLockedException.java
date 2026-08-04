package com.uit.petrescueapi.domain.exception;

/**
 * Exception thrown when an account is temporarily locked due to too many failed login attempts.
 */
public class AccountLockedException extends BusinessException {

    private final long remainingSeconds;

    public AccountLockedException(long remainingSeconds) {
        super("Your account is temporarily locked. Please try again after " + formatRemainingTime(remainingSeconds));
        this.remainingSeconds = remainingSeconds;
    }

    public AccountLockedException(String message) {
        super(message);
        this.remainingSeconds = -1;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    private static String formatRemainingTime(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        }
        long minutes = seconds / 60;
        return minutes + " minute" + (minutes > 1 ? "s" : "");
    }
}
