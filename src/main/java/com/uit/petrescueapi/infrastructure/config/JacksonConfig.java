package com.uit.petrescueapi.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper configuration. Every {@link java.time.LocalDateTime} goes out as UTC with a
 * {@code Z} and comes in with any offset; see {@link com.uit.petrescueapi.application.support.AppTime}.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // registered after JavaTimeModule so it replaces its LocalDateTime handling
        mapper.registerModule(new SimpleModule("utc-local-date-time")
                .addSerializer(java.time.LocalDateTime.class, new UtcLocalDateTimeSerializer())
                .addDeserializer(java.time.LocalDateTime.class, new FlexibleLocalDateTimeDeserializer()));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
