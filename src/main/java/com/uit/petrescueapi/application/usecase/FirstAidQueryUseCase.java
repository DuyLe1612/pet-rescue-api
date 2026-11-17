package com.uit.petrescueapi.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.dto.rescue.FirstAidGuidesDto;
import com.uit.petrescueapi.application.port.query.FirstAidQueryPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Reads the guides once at startup; they only change with a release. */
@Service
public class FirstAidQueryUseCase implements FirstAidQueryPort {

    private static final String RESOURCE = "first-aid/guides-vi.json";

    private final FirstAidGuidesDto guides;

    public FirstAidQueryUseCase(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            this.guides = objectMapper.readValue(in, FirstAidGuidesDto.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    @Override
    public FirstAidGuidesDto findGuides() {
        return guides;
    }
}
