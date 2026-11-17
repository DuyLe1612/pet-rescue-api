package com.uit.petrescueapi;

import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * scripts/demo-data runs against the real schema: every migration applied, every V29 rule on.
 * Keeps the demo script working as tables change.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "app.rescue.maintenance-delay-ms=3600000"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoDataScriptsIntegrationTest {

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

    @Autowired JdbcTemplate jdbc;
    @MockitoBean CloudStoragePort cloud;
    @MockitoBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @Test
    @Order(1)
    void theSeedLoadsTheAmountsTheReadmePromises() throws Exception {
        run("seed-postgres.sql");

        assertThat(count("SELECT COUNT(*) FROM users WHERE user_code LIKE 'U-D%'")).isEqualTo(47);
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%'")).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%' AND status = 'PENDING'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%'")).isEqualTo(60);
        assertThat(count("SELECT COUNT(*) FROM rescue_cases WHERE case_code LIKE 'R-D%'")).isEqualTo(80);
        assertThat(count("SELECT COUNT(*) FROM adoption_applications WHERE adoption_code LIKE 'A-D%'")).isEqualTo(28);
        assertThat(count("SELECT COUNT(*) FROM rescue_case_completions c JOIN rescue_cases r ON r.case_id = c.case_id WHERE r.case_code LIKE 'R-D%'")).isEqualTo(36);
        assertThat(count("SELECT COUNT(*) FROM pet_medical_records m JOIN pets p ON p.pet_id = m.pet_id WHERE p.pet_code LIKE 'P-D%'")).isEqualTo(64);
    }

    @Test
    @Order(2)
    void everyPetHasACatalogBreedAPhotoAndAShelterThatIsOpen() {
        assertThat(count("SELECT COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%' AND breed_id IS NULL")).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM pets p WHERE p.pet_code LIKE 'P-D%'
                  AND NOT EXISTS (SELECT 1 FROM pet_media m WHERE m.pet_id = p.pet_id AND m.is_primary)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM pets p JOIN organizations o ON o.organization_id = p.shelter_id
                WHERE p.pet_code LIKE 'P-D%' AND o.status <> 'ACTIVE'
                """)).isZero();
        // a breed always belongs to the pet's own species
        assertThat(count("""
                SELECT COUNT(*) FROM pets p JOIN pet_breeds b ON b.breed_id = p.breed_id
                WHERE p.pet_code LIKE 'P-D%' AND b.species_id <> p.species_id
                """)).isZero();
    }

    @Test
    @Order(3)
    void storedValuesAreCanonical() {
        assertThat(jdbc.queryForList("SELECT DISTINCT species FROM rescue_cases WHERE case_code LIKE 'R-D%'", String.class))
                .containsExactlyInAnyOrder("Dog", "Cat");
        assertThat(jdbc.queryForList("SELECT DISTINCT public_id FROM media_files WHERE folder = 'demo'", String.class))
                .allMatch(url -> url.startsWith("https://images.unsplash.com/photo-"));
        Map<String, Object> names = jdbc.queryForMap("SELECT full_name, user_code FROM users WHERE username = 'demo.owner1'");
        assertThat(names).containsEntry("full_name", "Trần Thị Hồng Nhung").containsEntry("user_code", "U-DO0001");
    }

    @Test
    @Order(4)
    void runningTheSeedAgainChangesNothing() throws Exception {
        long before = count("SELECT COUNT(*) FROM rescue_cases") + count("SELECT COUNT(*) FROM pets")
                + count("SELECT COUNT(*) FROM pet_medical_records");
        run("seed-postgres.sql");
        long after = count("SELECT COUNT(*) FROM rescue_cases") + count("SELECT COUNT(*) FROM pets")
                + count("SELECT COUNT(*) FROM pet_medical_records");
        assertThat(after).isEqualTo(before);
    }

    @Test
    @Order(5)
    void cleanupRemovesTheDemoDataAndKeepsRealData() throws Exception {
        UUID real = UUID.randomUUID();
        jdbc.update("INSERT INTO users (user_id, username, email, status, email_verified, user_code) VALUES (?, 'real.person', 'real@example.test', 'ACTIVE', TRUE, 'U-0001')", real);

        run("cleanup-postgres.sql");

        assertThat(count("SELECT COUNT(*) FROM users WHERE user_code LIKE 'U-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM rescue_cases WHERE case_code LIKE 'R-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM users WHERE user_id = '" + real + "'")).isEqualTo(1);
    }

    private void run(String script) throws Exception {
        jdbc.execute(Files.readString(Path.of("scripts/demo-data", script)));
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
