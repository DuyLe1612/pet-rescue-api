package com.uit.petrescueapi.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisCatalogJdbcAdapterTest {

    @Test
    void listsOnlyNonDeletedSpeciesAndTagsSorted() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(db);
        jdbc.getJdbcTemplate().execute("CREATE TABLE pet_species (name VARCHAR(100), is_deleted BOOLEAN)");
        jdbc.getJdbcTemplate().execute("CREATE TABLE tags (code VARCHAR(100), is_deleted BOOLEAN)");
        jdbc.getJdbcTemplate().execute("INSERT INTO pet_species VALUES ('Dog', FALSE), ('Cat', FALSE), ('Dodo', TRUE)");
        jdbc.getJdbcTemplate().execute("INSERT INTO tags VALUES ('rescue', FALSE), ('injured', FALSE), ('old', TRUE), (NULL, FALSE)");

        AnalysisCatalogJdbcAdapter adapter = new AnalysisCatalogJdbcAdapter(jdbc);

        assertThat(adapter.speciesNames()).containsExactly("Cat", "Dog");
        assertThat(adapter.tagCodes()).containsExactly("injured", "rescue");
    }
}
