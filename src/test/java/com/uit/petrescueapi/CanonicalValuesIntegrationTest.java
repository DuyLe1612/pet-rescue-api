package com.uit.petrescueapi;

import com.jayway.jsonpath.JsonPath;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.support.CanonicalText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One stored form per value (V29 and the request normalization), on real PostGIS: the SQL key
 * function agrees with the Java one, the database refuses spelling duplicates, and a request
 * written in Vietnamese or in odd case is stored as the canonical English value.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "app.rescue.maintenance-delay-ms=3600000"
})
@AutoConfigureMockMvc
class CanonicalValuesIntegrationTest {

    private static final String DB = "petrescue";

    @SuppressWarnings("resource")
    static final GenericContainer<?> POSTGIS = new GenericContainer<>("postgis/postgis:16-3.5-alpine")
            .withEnv("POSTGRES_DB", DB)
            .withEnv("POSTGRES_USER", DB)
            .withEnv("POSTGRES_PASSWORD", DB)
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static {
        POSTGIS.start();
        MONGO.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://" + POSTGIS.getHost() + ":" + POSTGIS.getMappedPort(5432) + "/" + DB);
        registry.add("spring.datasource.username", () -> DB);
        registry.add("spring.datasource.password", () -> DB);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CloudStoragePort cloud;
    @MockitoBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @BeforeEach
    void cloudBuildsUrls() {
        when(cloud.buildUrl(anyString())).thenAnswer(i -> "https://cdn.test/" + i.getArgument(0));
    }

    // ── The SQL key is the Java key ─────────────────

    @Test
    void sqlAndJavaKeysAgree() {
        List<String> samples = List.of("Dog", " DOG ", "Chó", "Đực", "Chuột lang", "Khẩn cấp", "Shih-Tzu!!",
                "in progress", "Tìm chủ", "MÈO CẢNH", "!!!");
        for (String sample : samples) {
            String sql = jdbc.queryForObject("SELECT canonical_key(?)", String.class, sample);
            assertThat(sql).as(sample).isEqualTo(CanonicalText.key(sample));
        }
    }

    // ── Spelling duplicates are refused ─────────────

    @Test
    void aSecondSpeciesSpelledDifferentlyIsRefused() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO pet_species (species_id, name) VALUES (?, 'DOG ')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aSecondBreedSpelledDifferentlyIsRefused() {
        UUID dog = jdbc.queryForObject("SELECT species_id FROM pet_species WHERE name = 'Dog'", UUID.class);
        String breed = jdbc.queryForObject("SELECT name FROM pet_breeds WHERE species_id = ? AND name = 'Shih Tzu'", String.class, dog);
        assertThat(breed).isEqualTo("Shih Tzu");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO pet_breeds (breed_id, species_id, name) VALUES (?, ?, 'shih-tzu')",
                UUID.randomUUID(), dog)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tagsAreSeededAsSlugsAndASecondSpellingIsRefused() {
        assertThat(jdbc.queryForList("SELECT code FROM tags WHERE is_deleted = FALSE", String.class))
                .contains("rescue", "adoption", "lost-pet");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO tags (tag_id, code) VALUES (?, 'Lost Pet')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enumColumnsOnlyTakeConstantNames() {
        UUID reporter = person();
        assertThatThrownBy(() -> jdbc.update("UPDATE users SET gender = 'male' WHERE user_id = ?", reporter))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE users SET gender = 'MALE' WHERE user_id = ?", reporter);
    }

    @Test
    void aRescueCaseSpeciesMustBeACatalogName() {
        UUID caseId = report(person(), "Dog");
        assertThatThrownBy(() -> jdbc.update("UPDATE rescue_cases SET species = 'chó' WHERE case_id = ?", caseId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Requests in any spelling are stored canonically ──

    @Test
    void aCaseReportedInVietnameseIsStoredInEnglish() throws Exception {
        UUID caseId = report(person(), "chó", "\"size\":\"nhỏ\",\"priority\":\"khẩn cấp\"");

        assertThat(jdbc.queryForMap("SELECT species, size, priority FROM rescue_cases WHERE case_id = ?", caseId))
                .containsEntry("species", "Dog")
                .containsEntry("size", "SMALL")
                .containsEntry("priority", "CRITICAL");
    }

    @Test
    void anUnknownSpeciesIsStoredAsOther() {
        UUID caseId = report(person(), "kỳ nhông");
        assertThat(jdbc.queryForObject("SELECT species FROM rescue_cases WHERE case_id = ?", String.class, caseId))
                .isEqualTo("Other");
    }

    @Test
    void filtersFindTheCaseWhateverSpellingIsUsed() throws Exception {
        UUID reporter = person();
        UUID caseId = report(reporter, "Mèo");

        for (String spelling : List.of("Cat", "cat", "MÈO", "meo")) {
            call(get("/api/v1/rescue-cases").param("species", spelling).param("pageSize", "100")
                    .param("priority", "high"), reporter)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[?(@.caseId == '" + caseId + "')]").exists());
        }
    }

    @Test
    void anUnknownEnumIsA400ThatListsTheValidValues() throws Exception {
        call(post("/api/v1/rescue-cases"), person(), body("Dog", "\"size\":\"huge\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SMALL, MEDIUM, LARGE")));
    }

    // ── Helpers ─────────────────────────────────────

    private UUID report(UUID reporter, String species) {
        return report(reporter, species, "\"priority\":\"HIGH\"");
    }

    private UUID report(UUID reporter, String species, String extra) {
        try {
            String response = call(post("/api/v1/rescue-cases"), reporter, body(species, extra))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            return UUID.fromString(JsonPath.read(response, "$.data.caseId"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String body(String species, String extra) {
        return """
                {"species":"%s",%s,"description":"Bị thương","latitude":10.5,"longitude":106.7,
                 "locationText":"Cầu Thị Nghè","provinceName":"Hồ Chí Minh","provinceCode":"79","contactPhone":"0900000000"}
                """.formatted(species, extra);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, UUID caller) throws Exception {
        return mvc.perform(request.with(user(caller.toString()).roles("USER")));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, UUID caller, String body) throws Exception {
        return mvc.perform(request.with(user(caller.toString()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID person() {
        UUID id = UUID.randomUUID();
        String tag = id.toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO users (user_id, username, email, status, email_verified, user_code)
                VALUES (?, ?, ?, 'ACTIVE', TRUE, ?)
                """, id, "u-" + tag, tag + "@petrescue.test", tag);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, role_id FROM roles WHERE code = 'USER'", id);
        return id;
    }
}
