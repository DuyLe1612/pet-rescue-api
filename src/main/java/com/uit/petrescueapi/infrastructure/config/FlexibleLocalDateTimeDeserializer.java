package com.uit.petrescueapi.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.uit.petrescueapi.application.support.AppTime;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Reads every {@link LocalDateTime} in a request body. Accepts {@code Z} or an offset, a time
 * without one (taken as UTC), or a date alone (start of that day in Vietnam). See {@link AppTime}.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AppTime.fromWire(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidFormatException(parser,
                    "Invalid date-time. Use ISO-8601 with an offset, e.g. '2026-11-12T10:00:00Z' or '2026-11-12T17:00:00+07:00'",
                    value,
                    LocalDateTime.class);
        }
    }
}
