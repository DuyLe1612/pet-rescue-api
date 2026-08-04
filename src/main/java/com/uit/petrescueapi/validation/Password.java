package com.uit.petrescueapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Password strength validation annotation.
 * 
 * <p>Requires password to have:
 * <ul>
 *   <li>At least 8 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special character (@$!%*?&)</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Password {
    String message() default "Password must be at least 8 characters and contain uppercase, lowercase, digit";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
