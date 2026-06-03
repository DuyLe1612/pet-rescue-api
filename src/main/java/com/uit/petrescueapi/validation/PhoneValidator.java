package com.uit.petrescueapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    // Very small E.164-like validation: optional +, then country code and subscriber (max 15 digits)
    private static final String E164_REGEX = "^\\+?[0-9][0-9]{1,14}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // leave @NotBlank for required checks
        String candidate = value.replaceAll("[\\s()\\-\\.]+", "");
        return candidate.matches(E164_REGEX);
    }
}
