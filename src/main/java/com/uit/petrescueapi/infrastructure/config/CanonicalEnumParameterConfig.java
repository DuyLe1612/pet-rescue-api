package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.support.EnumAliases;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Query and path parameters get the same enum reading as request bodies
 * ({@code ?priority=khan cap} is {@code CRITICAL}). Replaces Spring's case-sensitive default; an
 * unknown value is still a 400 through {@code MethodArgumentTypeMismatchException}.
 */
@Configuration
public class CanonicalEnumParameterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.removeConvertible(String.class, Enum.class);
        registry.addConverterFactory(new StringToCanonicalEnum());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static final class StringToCanonicalEnum implements ConverterFactory<String, Enum> {

        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            Class<?> enumType = targetType;
            while (enumType != null && !enumType.isEnum()) {
                enumType = enumType.getSuperclass();
            }
            Class<Enum> type = (Class<Enum>) enumType;
            return source -> (T) EnumAliases.parse(type, source);
        }
    }
}
