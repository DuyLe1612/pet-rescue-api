package com.uit.petrescueapi.infrastructure.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.uit.petrescueapi.application.support.AppTime;

import java.io.IOException;
import java.time.LocalDateTime;

/** Writes every {@link LocalDateTime} as UTC with a trailing {@code Z}, so no client has to guess. */
public class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeString(AppTime.toWire(value));
    }

    @Override
    public Class<LocalDateTime> handledType() {
        return LocalDateTime.class;
    }
}
