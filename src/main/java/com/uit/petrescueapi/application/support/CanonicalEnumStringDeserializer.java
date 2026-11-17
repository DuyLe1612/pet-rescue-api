package com.uit.petrescueapi.application.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Reads a {@link CanonicalEnum} field: the text sent becomes the matching constant's name.
 * Without the annotation (should not happen) the text passes through unchanged.
 */
public class CanonicalEnumStringDeserializer extends StdDeserializer<String> implements ContextualDeserializer {

    private final Class<? extends Enum<?>> enumType;

    public CanonicalEnumStringDeserializer() {
        this(null);
    }

    private CanonicalEnumStringDeserializer(Class<? extends Enum<?>> enumType) {
        super(String.class);
        this.enumType = enumType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        CanonicalEnum annotation = property == null ? null : property.getAnnotation(CanonicalEnum.class);
        return annotation == null ? this : new CanonicalEnumStringDeserializer(annotation.value());
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_STRING) {
            return (String) ctxt.handleUnexpectedToken(String.class, p);
        }
        String raw = p.getText();
        if (enumType == null) {
            return raw;
        }
        try {
            return EnumAliases.canonicalName(enumType, raw);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(raw, enumType, e.getMessage());
        }
    }
}
