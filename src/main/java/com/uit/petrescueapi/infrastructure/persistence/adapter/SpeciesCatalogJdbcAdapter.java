package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.SpeciesCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpeciesCatalogJdbcAdapter implements SpeciesCatalogPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<SpeciesNames> species() {
        return jdbc.query("SELECT name, name_vi FROM pet_species WHERE is_deleted = FALSE AND name IS NOT NULL",
                Map.of(), (rs, i) -> new SpeciesNames(rs.getString("name"), rs.getString("name_vi")));
    }

    @Override
    public List<SpeciesAlias> aliases() {
        return jdbc.query("""
                        SELECT a.alias_key, s.name
                        FROM pet_species_aliases a JOIN pet_species s ON s.species_id = a.species_id
                        WHERE s.is_deleted = FALSE
                        """,
                Map.of(), (rs, i) -> new SpeciesAlias(rs.getString("alias_key"), rs.getString("name")));
    }
}
