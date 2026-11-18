package com.uit.petrescueapi.application.support;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a text field that holds an enum value. Whatever the client sends ("male", "Đực", "MALE")
 * reaches the code as the constant's name ("MALE"), and anything else is refused with a 400 that
 * lists the valid values. Use on request DTO fields that are still {@code String}.
 *
 * <pre>{@code @CanonicalEnum(Gender.class) private String gender;}</pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = CanonicalEnumStringDeserializer.class)
public @interface CanonicalEnum {

    Class<? extends Enum<?>> value();
}
