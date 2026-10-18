package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Species and tag catalogs for the AI analysis. Sorted so the prompt built from them is
 * byte-stable between calls (prompt caching).
 */
@Component
@RequiredArgsConstructor
public class AnalysisCatalogJdbcAdapter implements AnalysisCatalogPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<String> speciesNames() {
        return jdbc.queryForList("SELECT name FROM pet_species WHERE is_deleted = FALSE AND name IS NOT NULL ORDER BY name",
                Map.of(), String.class);
    }

    @Override
    public List<String> tagCodes() {
        return jdbc.queryForList("SELECT code FROM tags WHERE is_deleted = FALSE AND code IS NOT NULL ORDER BY code",
                Map.of(), String.class);
    }
}
