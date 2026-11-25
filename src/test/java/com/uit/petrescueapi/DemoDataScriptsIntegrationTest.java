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
 * scripts/demo-data runs against the real schema: every migration applied, every V35 rule on.
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
    @Autowired com.uit.petrescueapi.application.port.command.AdoptionCommandPort adoptions;
    @Autowired com.uit.petrescueapi.application.port.query.ChatQueryPort chats;
    @Autowired com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort postAnalyses;
    @Autowired org.springframework.data.mongodb.core.MongoTemplate mongo;
    @MockitoBean CloudStoragePort cloud;
    @MockitoBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @Test
    @Order(1)
    void theSeedLoadsTheAmountsTheReadmePromises() throws Exception {
        run("seed-postgres.sql");

        assertThat(count("SELECT COUNT(*) FROM users WHERE user_code LIKE 'U-D%'")).isEqualTo(47);
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%'")).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%' AND status = 'PENDING'")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%' AND image_url IS NULL"))
                .as("every centre has a photo people can recognise it by").isZero();
        assertThat(count("SELECT COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%'")).isEqualTo(62);
        assertThat(count("SELECT COUNT(*) FROM rescue_cases WHERE case_code LIKE 'R-D%'")).isEqualTo(83);
        assertThat(count("SELECT COUNT(*) FROM adoption_applications WHERE adoption_code LIKE 'A-D%'")).isEqualTo(37);
        assertThat(count("SELECT COUNT(*) FROM rescue_case_completions c JOIN rescue_cases r ON r.case_id = c.case_id WHERE r.case_code LIKE 'R-D%'")).isEqualTo(37);
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
    void everyScenarioOfTheReadmeHasItsData() {
        // demo.user1
        assertThat(count("SELECT COUNT(*) FROM adopter_profiles WHERE user_id = " + id("user", 1))).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM pets_current_owner WHERE owner_id = " + id("user", 1))).isGreaterThanOrEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM adoption_followups WHERE adopter_id = " + id("user", 1) + " AND status = 'SENT'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM case_match_suggestions WHERE status = 'PENDING' AND lost_case_id IN "
                + "(SELECT case_id FROM rescue_cases WHERE case_type = 'LOST' AND reported_by = " + id("user", 1) + ")")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM rescue_case_completions c JOIN rescue_cases r ON r.case_id = c.case_id "
                + "WHERE c.status = 'PENDING' AND r.reported_by = " + id("user", 1))).isPositive();
        // demo.owner1
        assertThat(count("SELECT COUNT(*) FROM followup_alerts WHERE status <> 'RESOLVED' AND organization_id = " + id("org", 1))).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM reclaim_logs WHERE organization_id = " + id("org", 1))).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM rescue_case_claims WHERE status = 'ACTIVE' AND organization_id = " + id("org", 1))).isEqualTo(1);
        // demo.admin
        assertThat(count("SELECT COUNT(DISTINCT case_id) FROM rescue_case_flags")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM rescue_case_completions WHERE status = 'DISPUTED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM banners WHERE is_active")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM rescue_case_triage")).isPositive();
        assertThat(count("SELECT COUNT(*) FROM pet_traits t JOIN pets p ON p.pet_id = t.pet_id WHERE p.pet_code LIKE 'P-D%'")).isEqualTo(60);
    }

    @Test
    @Order(5)
    void theBackfillScoresOpenApplicationsWhereBothSidesCanBeCompared() {
        adoptions.backfillMatches(100);

        // the five applications at organization 1 from users with an adopter profile
        assertThat(count("SELECT COUNT(*) FROM adoption_applications WHERE adoption_code BETWEEN 'A-D9001' AND 'A-D9005' AND match_level IS NOT NULL"))
                .isEqualTo(5);
        // user21 never filled in the profile: nothing to compare yet
        assertThat(count("SELECT COUNT(*) FROM adoption_applications WHERE adoption_code = 'A-D9006' AND match_level IS NULL")).isEqualTo(1);
        // user3 is allergic to dogs, user4 lives where only small pets are allowed
        assertThat(text("SELECT match_warnings FROM adoption_applications WHERE adoption_code = 'A-D9003'")).contains("ALLERGY");
        assertThat(text("SELECT match_warnings FROM adoption_applications WHERE adoption_code = 'A-D9004'")).contains("BUILDING_RULE");
        assertThat(text("SELECT match_level FROM adoption_applications WHERE adoption_code = 'A-D9001'")).isIn("HIGH", "MEDIUM");
    }

    @Test
    @Order(6)
    void theApiReadsTheChatsAndPostAnalysesOfTheMongoSeed() throws Exception {
        MONGO.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath("scripts/demo-data/seed-mongo.js"), "/seed.js");
        String db = mongo.getDb().getName();
        for (int pass = 0; pass < 2; pass++) {
            var seeded = MONGO.execInContainer("mongosh", "--quiet", db, "--eval", "var DB_NAME = '" + db + "'", "/seed.js");
            assertThat(seeded.getExitCode()).as(seeded.getStderr()).isZero();
        }

        UUID user1 = jdbc.queryForObject("SELECT " + id("user", 1), UUID.class);
        var inbox = chats.listConversationsByCursor(user1, null, 20).getItems();
        assertThat(inbox).hasSize(3);
        assertThat(inbox).extracting(c -> c.getType()).containsExactlyInAnyOrder("GENERAL", "ADOPTION", "RESCUE");
        assertThat(inbox).allMatch(c -> c.getOtherUserName() != null && c.getLastMessage() != null);
        assertThat(inbox.stream().mapToInt(c -> c.getUnread()).sum()).isEqualTo(3);
        var adoptionChat = inbox.stream().filter(c -> "ADOPTION".equals(c.getType())).findFirst().orElseThrow();
        assertThat(chats.listMessagesByCursor(adoptionChat.getId(), user1, null, null, 20, "before").getItems()).hasSize(3);

        UUID waiting = jdbc.queryForObject("SELECT " + id("post", 9001), UUID.class);
        var analysis = postAnalyses.getAnalysis(waiting);
        assertThat(analysis.getStatus()).hasToString("ANALYZED");
        assertThat(analysis.getSuggestion().category()).hasToString("LOST");
        assertThat(analysis.getSuggestion().province()).isEqualTo("Thành phố Hồ Chí Minh");
    }

    @Test
    @Order(7)
    void runningTheSeedAgainChangesNothing() throws Exception {
        long before = count("SELECT COUNT(*) FROM rescue_cases") + count("SELECT COUNT(*) FROM pets")
                + count("SELECT COUNT(*) FROM pet_medical_records");
        run("seed-postgres.sql");
        long after = count("SELECT COUNT(*) FROM rescue_cases") + count("SELECT COUNT(*) FROM pets")
                + count("SELECT COUNT(*) FROM pet_medical_records");
        assertThat(after).isEqualTo(before);
    }

    @Test
    @Order(8)
    void cleanupRemovesTheDemoDataAndKeepsRealData() throws Exception {
        UUID real = UUID.randomUUID();
        jdbc.update("INSERT INTO users (user_id, username, email, status, email_verified, user_code, full_name) VALUES (?, 'real.person', 'real@example.test', 'ACTIVE', TRUE, 'U-0001', 'Người thật')", real);
        // During a demo, user1 reports a case (the API gives it a real code) and a real person keeps the pet from it.
        jdbc.update("""
                INSERT INTO rescue_cases (case_id, case_code, reported_by, species, location, status, reported_at, created_at)
                SELECT gen_random_uuid(), 'R-0001', reported_by, species, location, 'RESCUED', now(), now()
                FROM rescue_cases WHERE case_code = 'R-D0030'
                """);
        jdbc.update("""
                INSERT INTO pets (pet_id, pet_code, name, species_id, status, rescue_case_id, created_at)
                SELECT gen_random_uuid(), 'P-0001', 'Bé thật', species_id, 'ADOPTED',
                       (SELECT case_id FROM rescue_cases WHERE case_code = 'R-0001'), now()
                FROM pets WHERE pet_code = 'P-D0001'
                """);

        run("cleanup-postgres.sql");

        assertThat(count("SELECT COUNT(*) FROM users WHERE user_code LIKE 'U-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM pets WHERE pet_code LIKE 'P-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM rescue_cases WHERE case_code LIKE 'R-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE organization_code LIKE 'O-D%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM users WHERE user_id = '" + real + "'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM rescue_cases WHERE case_code = 'R-0001'")).as("a demo user's case goes too").isZero();
        assertThat(count("SELECT COUNT(*) FROM pets WHERE pet_code = 'P-0001' AND rescue_case_id IS NULL")).as("the real pet stays").isEqualTo(1);
        for (String table : new String[] {"adopter_profiles", "pet_traits", "adoption_followups", "followup_alerts", "rescue_case_flags", "rescue_case_triage", "case_match_suggestions", "banners"}) {
            assertThat(count("SELECT COUNT(*) FROM " + table)).as(table).isZero();
        }
    }

    /** The id seed-postgres.sql gives a demo row, as SQL. */
    private static String id(String kind, int n) {
        return "md5('pet-rescue-demo:" + kind + ":" + n + "')::uuid";
    }

    private String text(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    private void run(String script) throws Exception {
        jdbc.execute(Files.readString(Path.of("scripts/demo-data", script)));
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
