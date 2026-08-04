package com.uit.petrescueapi.domain.exception;

/**
 * Exception thrown when attempting to login to a permanently banned account.
 */
public class AccountBannedException extends BusinessException {

    public AccountBannedException() {
        super("Your account has been permanently banned. Please contact support for assistance.");
    }

    public AccountBannedException(String message) {
        super(message);
    }
}
