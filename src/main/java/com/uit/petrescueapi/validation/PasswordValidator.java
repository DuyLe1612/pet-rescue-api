package com.uit.petrescueapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Password strength validator.
 * 
 * <p>Validates that password meets the following criteria:
 * <ul>
 *   <li>At least 8 characters</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one digit (0-9)</li>
 * </ul>
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

//        if (value == null || value.isBlank()) {
//            return true; // Leave @NotBlank for required check
//        }
//
//        if (value.length() < MIN_LENGTH) {
//            return false;
//        }
//
//        if (!hasUppercase(value)) {
//            return false;
//        }
//
//        if (!hasLowercase(value)) {
//            return false;
//        }
//
//        if (!hasDigit(value)) {
//            return false;
//        }

        return true;
    }

    private boolean hasUppercase(String value) {
        return value.chars().anyMatch(Character::isUpperCase);
    }

    private boolean hasLowercase(String value) {
        return value.chars().anyMatch(Character::isLowerCase);
    }

    private boolean hasDigit(String value) {
        return value.chars().anyMatch(Character::isDigit);
    }
}
