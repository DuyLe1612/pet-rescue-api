package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.support.AppTime;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;

/**
 * Query parameters such as {@code cursor} follow the same rule as bodies, so a client can send back
 * the {@code ...Z} value it received. Added after Spring's defaults, so it takes precedence.
 */
@Configuration
public class DateTimeParameterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, LocalDateTime>() {
            @Override
            public LocalDateTime convert(String source) {
                return source.isBlank() ? null : AppTime.fromWire(source);
            }
        });
    }
}
