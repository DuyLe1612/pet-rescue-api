package com.uit.petrescueapi.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.uit.petrescueapi.application.support.EnumAliases;

import java.io.IOException;

/**
 * Every enum in a request body is read through {@link EnumAliases}: any case, with or without
 * diacritics, or a known Vietnamese label. Unknown text is a 400 that lists the valid values.
 * Enums of this project only; library enums keep Jackson's own handling.
 */
public class CanonicalEnumModule extends SimpleModule {

    private static final String OWN_PACKAGE = "com.uit.petrescueapi.";

    public CanonicalEnumModule() {
        super("canonical-enums");
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
                                                              BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                Class<?> raw = type.getRawClass();
                if (!raw.isEnum() || !raw.getName().startsWith(OWN_PACKAGE)) {
                    return deserializer;
                }
                return new Deserializer((Class) raw);
            }
        });
    }

    private static final class Deserializer<E extends Enum<E>> extends StdDeserializer<E> {

        private final Class<E> type;

        private Deserializer(Class<E> type) {
            super(type);
            this.type = type;
        }

        @Override
        public E deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.VALUE_STRING) {
                return (E) ctxt.handleUnexpectedToken(type, p);
            }
            String raw = p.getText();
            try {
                return EnumAliases.parse(type, raw);
            } catch (IllegalArgumentException e) {
                throw ctxt.weirdStringException(raw, type, e.getMessage());
            }
        }
    }
}
