package com.uit.petrescueapi;

import com.jayway.jsonpath.JsonPath;
import com.uit.petrescueapi.application.port.command.RescueMaintenancePort;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rescue workflow over HTTP against real PostGIS and MongoDB, with Flyway applying every migration
 * and Hibernate validating the schema. The names are the scenarios of agent/13-design-rescue-claim.md.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "app.rescue.maintenance-delay-ms=3600000"
})
@AutoConfigureMockMvc
class RescueWorkflowIntegrationTest {

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
    JdbcTemplate jdbc;
    @Autowired MongoTemplate mongo;
    @Autowired RescueMaintenancePort maintenance;

    /** The people below are field initializers, which run before Spring injects {@code jdbc}. */
    private final java.util.List<String[]> pendingUsers = new java.util.ArrayList<>();

    private final UUID an = person("an");
    private final UUID binh = person("binh");
    private final UUID chi = person("chi");

    // ── Whole journeys ──────────────────────────────

    @Test
    void personRescuesReporterConfirmsRescuerKeepsThePet() throws Exception {
        UUID caseId = report(an);

        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), binh, "{}").andExpect(status().isBadRequest());
        claim(caseId, binh);
        claim(caseId, chi);

        // the phone number goes only to people who need it
        call(get("/api/v1/rescue-cases/" + caseId), binh).andExpect(jsonPath("$.data.contactPhone").value("0900000000"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.activeClaimCount").value(2));
        mvc.perform(get("/api/v1/rescue-cases/" + caseId)).andExpect(jsonPath("$.data.contactPhone").doesNotExist());
        call(get("/api/v1/rescue-cases/" + caseId), person("stranger")).andExpect(jsonPath("$.data.contactPhone").doesNotExist());
        call(get("/api/v1/rescue-cases/" + caseId + "/claims"), an).andExpect(jsonPath("$.data", hasSize(2)));
        call(get("/api/v1/users/me/rescue-claims"), binh).andExpect(jsonPath("$.data", hasSize(1)));

        UUID completionId = complete(caseId, binh, "KEPT_BY_RESCUER");
        call(post("/api/v1/rescue-cases/completions/" + completionId + "/confirm"), chi).andExpect(status().isForbidden());
        call(post("/api/v1/rescue-cases/completions/" + completionId + "/confirm"), an).andExpect(status().isOk());

        assertThat(caseStatus(caseId)).isEqualTo("CLOSED");
        assertThat(notificationCount(chi, "RESCUE_CASE_ENDED")).isEqualTo(1);       // Chi is told to stop
        assertThat(reputationOf(binh)).isEqualTo(30);

        call(post("/api/v1/rescue-cases/" + caseId + "/keep-pet"), chi, petBody()).andExpect(status().isForbidden());
        call(post("/api/v1/rescue-cases/" + caseId + "/keep-pet"), binh, petBody()).andExpect(status().isCreated());
        call(post("/api/v1/rescue-cases/" + caseId + "/keep-pet"), binh, petBody()).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT pet_id FROM rescue_cases WHERE case_id = ?", UUID.class, caseId)).isNotNull();
    }

    @Test
    void organizationClaimsReportsAndTakesIn() throws Exception {
        UUID org = organization("79");
        UUID staff = member(org, "STAFF");
        UUID otherStaff = member(organization("79"), "OWNER");
        UUID caseId = report(an);

        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), staff).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationId").value(org.toString()))
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), otherStaff).andExpect(status().isConflict());
        call(get("/api/v1/rescue-cases/" + caseId), staff).andExpect(jsonPath("$.data.contactPhone").value("0900000000"));

        // staff of the holding organization need no second opinion, and earn no points
        call(post("/api/v1/rescue-cases/" + caseId + "/intake"), staff, petBody()).andExpect(status().isConflict());
        complete(caseId, staff, "TEMP_HOLDING");
        assertThat(caseStatus(caseId)).isEqualTo("RESCUED");
        assertThat(reputationOf(staff)).isZero();

        call(post("/api/v1/rescue-cases/" + caseId + "/intake"), otherStaff, petBody()).andExpect(status().isForbidden());
        call(post("/api/v1/rescue-cases/" + caseId + "/intake"), staff, petBody()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("FOSTERING"));
        call(post("/api/v1/rescue-cases/" + caseId + "/intake"), staff, petBody()).andExpect(status().isConflict());
    }

    @Test
    void rescuerHandsTheAnimalToAnOrganizationFromTheQueue() throws Exception {
        String province = uniqueProvince();
        UUID staff = member(organization(province), "STAFF");
        UUID caseId = reportIn(an, province);
        UUID completionId = complete(caseId, binh, "HANDED_TO_ORG");
        call(post("/api/v1/rescue-cases/completions/" + completionId + "/confirm"), an).andExpect(status().isOk());
        assertThat(caseStatus(caseId)).isEqualTo("RESCUED");

        call(get("/api/v1/organizations/me/rescue-queue"), staff)
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].awaitingHandover").value(true));

        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), staff).andExpect(status().isOk());
        call(get("/api/v1/organizations/me/rescue-queue"), staff).andExpect(jsonPath("$.data.content", hasSize(0)));
        call(post("/api/v1/rescue-cases/" + caseId + "/intake"), staff, petBody()).andExpect(status().isCreated());
    }

    // ── Bad reports must not lock a case ────────────

    @Test
    void fakeReportIsDisputedRejectedAndTheCaseOpensAgain() throws Exception {
        UUID admin = admin();
        UUID caseId = report(an);
        UUID fake = complete(caseId, person("dung"), "TEMP_HOLDING");

        // a second report waits while the first is open
        complete(caseId, chi, "TEMP_HOLDING", 409);

        call(post("/api/v1/rescue-cases/completions/" + fake + "/dispute"), an, "{\"reason\":\"Ảnh lấy trên mạng\"}").andExpect(status().isOk());
        assertThat(completionStatus(fake)).isEqualTo("DISPUTED");
        assertThat(notificationCount(admin, "RESCUE_COMPLETION_NEEDS_REVIEW")).isEqualTo(1);
        assertThat(reviewQueueContains(admin, fake)).isTrue();

        // the reporter has had their say; only an admin settles it now
        call(post("/api/v1/rescue-cases/completions/" + fake + "/approve"), an).andExpect(status().isForbidden());
        UUID orgStaff = member(organization("79"), "STAFF");
        call(post("/api/v1/rescue-cases/completions/" + fake + "/reject"), orgStaff, "{\"reason\":\"x\"}").andExpect(status().isForbidden());
        call(post("/api/v1/rescue-cases/completions/" + fake + "/reject"), admin, "{\"reason\":\"Ảnh giả\"}").andExpect(status().isOk());

        assertThat(caseStatus(caseId)).isEqualTo("REPORTED");
        assertThat(reputationOf(an)).isZero();
        complete(caseId, chi, "TEMP_HOLDING");                                      // the real one goes through
    }

    @Test
    void reporterWhoRescuesCannotVouchForThemselves() throws Exception {
        UUID admin = admin();
        UUID caseId = report(an);
        UUID completionId = complete(caseId, an, "RELEASED");

        call(post("/api/v1/rescue-cases/completions/" + completionId + "/confirm"), an).andExpect(status().isForbidden());
        call(post("/api/v1/rescue-cases/completions/" + completionId + "/approve"), an).andExpect(status().isForbidden());
        assertThat(reviewQueueContains(admin, completionId)).isTrue();

        call(post("/api/v1/rescue-cases/completions/" + completionId + "/approve"), admin).andExpect(status().isOk());
        assertThat(caseStatus(caseId)).isEqualTo("CLOSED");
    }

    @Test
    void reportCannotUseSomeoneElsesPhotoAndLeavesNothingBehind() throws Exception {
        UUID caseId = report(an);
        String body = "{\"verificationImageIds\":[\"" + media(an) + "\"],\"confirmRescued\":true}";

        call(post("/api/v1/rescue-cases/" + caseId + "/complete"), binh, body).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rescue_case_completions WHERE case_id = ?", Long.class, caseId)).isZero();
    }

    @Test
    void reportsAreRefusedForEndedOrUnknownCases() throws Exception {
        UUID caseId = report(an);
        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), an, "{\"status\":\"CLOSED\",\"reason\":\"Đã tự giải quyết\"}").andExpect(status().isOk());

        complete(caseId, binh, "TEMP_HOLDING", 409);
        complete(UUID.randomUUID(), binh, "TEMP_HOLDING", 404);
    }

    // ── Claims ──────────────────────────────────────

    @Test
    void personalClaimsAreLimitedAndDoubleTapIsHarmless() throws Exception {
        UUID helper = person("helper");
        UUID first = report(an);
        claim(first, helper);
        claim(first, helper);                                                       // double tap
        assertThat(activeClaims(first)).isEqualTo(1);

        claim(report(an), helper);
        call(post("/api/v1/rescue-cases/" + report(an) + "/claim"), helper, ack()).andExpect(status().isConflict());
    }

    @Test
    void twoPeopleClaimingTogetherBothGetIn() throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 5; i++) {
                UUID caseId = report(an);
                UUID first = person("racer");
                UUID second = person("racer");
                var one = pool.submit(() -> call(post("/api/v1/rescue-cases/" + caseId + "/claim"), first, ack()).andReturn().getResponse().getStatus());
                var two = pool.submit(() -> call(post("/api/v1/rescue-cases/" + caseId + "/claim"), second, ack()).andReturn().getResponse().getStatus());

                assertThat(java.util.List.of(one.get(), two.get())).containsOnly(200);
                assertThat(activeClaims(caseId)).isEqualTo(2);
                assertThat(caseStatus(caseId)).isEqualTo("IN_PROGRESS");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void releasingLastClaimReopensTheCaseAndFlagsWhenHelpIsNeeded() throws Exception {
        String province = uniqueProvince();
        UUID staff = member(organization(province), "STAFF");
        UUID caseId = reportIn(an, province);
        claim(caseId, binh);
        claim(caseId, chi);

        call(post("/api/v1/rescue-cases/" + caseId + "/claim/release"), binh, "{\"reason\":\"NOT_FOUND\"}").andExpect(status().isOk());
        assertThat(caseStatus(caseId)).isEqualTo("IN_PROGRESS");                    // Chi is still on it

        call(post("/api/v1/rescue-cases/" + caseId + "/claim/release"), chi, "{\"reason\":\"NEED_HELP\"}").andExpect(status().isOk());
        assertThat(caseStatus(caseId)).isEqualTo("REPORTED");
        assertThat(notificationCount(an, "RESCUE_CLAIM_RELEASED")).isEqualTo(2);
        call(get("/api/v1/organizations/me/rescue-queue"), staff).andExpect(jsonPath("$.data.content[0].needsHelp").value(true));

        call(post("/api/v1/rescue-cases/" + caseId + "/claim/release"), chi, "{}").andExpect(status().isConflict());
        call(post("/api/v1/rescue-cases/" + caseId + "/claim/release"), chi, "{\"reason\":\"CASE_ENDED\"}").andExpect(status().isBadRequest());
    }

    @Test
    void organizationReleaseUnlocksTheCaseForOthers() throws Exception {
        UUID staff = member(organization("79"), "STAFF");
        UUID rival = member(organization("79"), "STAFF");
        UUID caseId = report(an);
        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), staff).andExpect(status().isOk());
        call(post("/api/v1/rescue-cases/" + caseId + "/claim/release"), staff, "{}").andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT organization_id FROM rescue_cases WHERE case_id = ?", UUID.class, caseId)).isNull();
        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), rival).andExpect(status().isOk());
    }

    @Test
    void claimOpensAChatBetweenClaimerAndReporter() throws Exception {
        UUID caseId = report(an);
        long before = mongo.getCollection("conversations").countDocuments(new Document("type", "RESCUE"));

        claim(caseId, binh);

        long deadline = System.currentTimeMillis() + 10_000;
        long after = before;
        while (after == before && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);      // the chat opens asynchronously after commit
            after = mongo.getCollection("conversations").countDocuments(new Document("type", "RESCUE"));
        }
        assertThat(after).isEqualTo(before + 1);
    }

    // ── Time-driven work ────────────────────────────

    @Test
    void maintenanceRemindsExpiresAndEscalates() throws Exception {
        UUID admin = admin();
        UUID soon = report(an);
        claim(soon, binh);
        jdbc.update("UPDATE rescue_case_claims SET expires_at = NOW() + INTERVAL '10 minutes' WHERE case_id = ?", soon);
        UUID late = report(an);
        claim(late, chi);
        jdbc.update("UPDATE rescue_case_claims SET expires_at = NOW() - INTERVAL '1 minute', reminded_at = NOW() WHERE case_id = ?", late);
        UUID overdue = report(an);
        UUID overdueReport = complete(overdue, person("dung"), "TEMP_HOLDING");
        jdbc.update("UPDATE rescue_case_completions SET created_at = NOW() - INTERVAL '3 days' WHERE completion_id = ?", overdueReport);

        maintenance.run();
        maintenance.run();                                                          // told once, not every minute

        assertThat(notificationCount(binh, "RESCUE_CLAIM_EXPIRING")).isEqualTo(1);
        assertThat(caseStatus(soon)).isEqualTo("IN_PROGRESS");

        assertThat(jdbc.queryForObject("SELECT status FROM rescue_case_claims WHERE case_id = ?", String.class, late)).isEqualTo("EXPIRED");
        assertThat(caseStatus(late)).isEqualTo("REPORTED");
        assertThat(notificationCount(an, "RESCUE_CLAIM_EXPIRED")).isEqualTo(1);

        assertThat(notificationCount(admin, "RESCUE_COMPLETION_NEEDS_REVIEW")).isEqualTo(1);
        assertThat(reviewQueueContains(admin, overdueReport)).isTrue();
    }

    // ── Case rules ──────────────────────────────────

    @Test
    void statusIsNotAShortcutPastTheWorkflow() throws Exception {
        UUID caseId = report(an);
        String closeNoReason = "{\"status\":\"CLOSED\"}";
        String close = "{\"status\":\"CLOSED\",\"reason\":\"Đã có người giúp\"}";

        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), an, "{\"status\":\"RESCUED\"}").andExpect(status().isForbidden());
        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), binh, close).andExpect(status().isForbidden());
        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), an, closeNoReason).andExpect(status().isBadRequest());

        claim(caseId, binh);
        UUID pending = complete(caseId, chi, "TEMP_HOLDING");
        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), an, close).andExpect(status().isConflict());

        call(post("/api/v1/rescue-cases/completions/" + pending + "/dispute"), an, "{\"reason\":\"x\"}").andExpect(status().isOk());
        call(post("/api/v1/rescue-cases/completions/" + pending + "/reject"), admin(), "{\"reason\":\"x\"}").andExpect(status().isOk());

        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), an, close).andExpect(status().isOk());
        assertThat(activeClaims(caseId)).isZero();
        assertThat(notificationCount(binh, "RESCUE_CASE_ENDED")).isEqualTo(1);

        call(put("/api/v1/rescue-cases/" + caseId), an, caseBody("79")).andExpect(status().isConflict());
    }

    @Test
    void movingTheCaseTellsPeopleOnTheWay() throws Exception {
        UUID caseId = report(an);
        claim(caseId, binh);

        call(put("/api/v1/rescue-cases/" + caseId), an, caseBody("79").replace("10.5", "10.9")).andExpect(status().isOk());

        assertThat(notificationCount(binh, "RESCUE_CASE_MOVED")).isEqualTo(1);
    }

    @Test
    void clientCannotAttachAPetWhenReportingOrEditing() throws Exception {
        UUID petId = UUID.randomUUID();
        String withPet = caseBody("79").replace("{", "{\"petId\":\"" + petId + "\",");
        String body = call(post("/api/v1/rescue-cases"), an, withPet).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID caseId = UUID.fromString(JsonPath.read(body, "$.data.caseId"));

        assertThat(jdbc.queryForObject("SELECT pet_id FROM rescue_cases WHERE case_id = ?", UUID.class, caseId)).isNull();
    }

    @Test
    void anonymousCannotWriteButCanRead() throws Exception {
        UUID caseId = report(an);
        mvc.perform(post("/api/v1/rescue-cases/" + caseId + "/claim")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/rescue-cases").contentType(MediaType.APPLICATION_JSON).content(caseBody("79")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/rescue-cases/" + caseId)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rescue-cases/completions/review-queue")).andExpect(status().is4xxClientError());
    }

    // ── Notifications ───────────────────────────────

    @Test
    void notificationsBelongToTheirOwner() throws Exception {
        UUID caseId = report(an);
        claim(caseId, binh);

        String list = call(get("/api/v1/notifications"), an)
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].type").value("RESCUE_CLAIMED"))
                .andExpect(jsonPath("$.data.content[0].data.caseId").value(caseId.toString()))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(list, "$.data.content[0].notificationId");

        call(get("/api/v1/notifications/unread-count"), an).andExpect(jsonPath("$.data.count").value(1));
        call(patch("/api/v1/notifications/" + id + "/read"), binh).andExpect(status().isNotFound());
        call(patch("/api/v1/notifications/" + id + "/read"), an).andExpect(status().isOk());
        call(patch("/api/v1/notifications/" + id + "/read"), an).andExpect(status().isOk());
        call(get("/api/v1/notifications/unread-count"), an).andExpect(jsonPath("$.data.count").value(0));
        call(get("/api/v1/notifications?unreadOnly=true"), an).andExpect(jsonPath("$.data.content", hasSize(0)));
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    // ── Helpers ─────────────────────────────────────

    private ResultActions call(MockHttpServletRequestBuilder request, UUID caller) throws Exception {
        return mvc.perform(request.with(user(caller.toString()).roles("USER")));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, UUID caller, String body) throws Exception {
        return mvc.perform(request.with(user(caller.toString()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID report(UUID reporter) throws Exception {
        return reportIn(reporter, "79");
    }

    private UUID reportIn(UUID reporter, String province) throws Exception {
        String body = call(post("/api/v1/rescue-cases"), reporter, caseBody(province))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.data.caseId"));
    }

    private static String caseBody(String province) {
        return "{\"species\":\"Dog\",\"priority\":\"HIGH\",\"description\":\"Bị thương\",\"latitude\":10.5,\"longitude\":106.7,"
                + "\"provinceCode\":\"" + province + "\",\"contactPhone\":\"0900000000\"}";
    }

    private void claim(UUID caseId, UUID user) throws Exception {
        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), user, ack()).andExpect(status().isOk());
    }

    private static String ack() {
        return "{\"safetyAcknowledged\":true}";
    }

    private UUID complete(UUID caseId, UUID user, String outcome) throws Exception {
        String body = call(post("/api/v1/rescue-cases/" + caseId + "/complete"), user, completionBody(user, outcome))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.data.completionId"));
    }

    private void complete(UUID caseId, UUID user, String outcome, int expectedStatus) throws Exception {
        call(post("/api/v1/rescue-cases/" + caseId + "/complete"), user, completionBody(user, outcome))
                .andExpect(status().is(expectedStatus));
    }

    private String completionBody(UUID user, String outcome) {
        return "{\"verificationImageIds\":[\"" + media(user) + "\"],\"confirmRescued\":true,\"outcome\":\"" + outcome + "\"}";
    }

    private String petBody() {
        UUID species = jdbc.queryForObject("SELECT species_id FROM pet_species LIMIT 1", UUID.class);
        return "{\"name\":\"Mực\",\"speciesId\":\"" + species + "\"}";
    }

    private UUID person(String name) {
        UUID id = UUID.randomUUID();
        String tag = id.toString().substring(0, 12);
        pendingUsers.add(new String[]{id.toString(), name + "-" + tag, tag});
        if (jdbc != null) {
            flushUsers();
        }
        return id;
    }

    @Autowired
    void insertPendingUsers(JdbcTemplate template) {
        this.jdbc = template;
        flushUsers();
    }

    private void flushUsers() {
        for (String[] user : pendingUsers) {
            jdbc.update("""
                    INSERT INTO users (user_id, username, email, status, email_verified, user_code)
                    VALUES (?, ?, ?, 'ACTIVE', TRUE, ?)
                    """, UUID.fromString(user[0]), user[1], user[1] + "@petrescue.test", user[2]);
            grant(UUID.fromString(user[0]), "USER");
        }
        pendingUsers.clear();
    }

    private void grant(UUID userId, String role) {
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, role_id FROM roles WHERE code = ?", userId, role);
    }

    private UUID admin() {
        UUID id = person("admin");
        grant(id, "ADMIN");
        return id;
    }

    private UUID organization(String province) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO organizations (organization_id, name, type, province_code, status, organization_code)
                VALUES (?, ?, 'SHELTER', ?, 'ACTIVE', ?)
                """, id, "Org " + id, province, id.toString().substring(0, 12));
        return id;
    }

    private UUID member(UUID organizationId, String role) {
        UUID id = person("member");
        jdbc.update("""
                INSERT INTO organization_members (organization_id, user_id, role, joined_at, status)
                VALUES (?, ?, ?, NOW(), 'ACTIVE')
                """, organizationId, id, role);
        return id;
    }

    private UUID media(UUID uploader) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO media_files (media_id, uploader_id, public_id, resource_type, status)
                VALUES (?, ?, ?, 'image', 'PERMANENT')
                """, id, uploader, "test/" + id);
        return id;
    }

    private String uniqueProvince() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String caseStatus(UUID caseId) {
        return jdbc.queryForObject("SELECT status FROM rescue_cases WHERE case_id = ?", String.class, caseId);
    }

    private String completionStatus(UUID completionId) {
        return jdbc.queryForObject("SELECT status FROM rescue_case_completions WHERE completion_id = ?", String.class, completionId);
    }

    private long activeClaims(UUID caseId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM rescue_case_claims WHERE case_id = ? AND status = 'ACTIVE'", Long.class, caseId);
    }

    private long notificationCount(UUID userId, String type) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?", Long.class, userId, type);
    }

    private int reputationOf(UUID userId) {
        return jdbc.queryForList("SELECT score FROM user_reputations WHERE user_id = ?", Integer.class, userId)
                .stream().findFirst().orElse(0);
    }

    private boolean reviewQueueContains(UUID admin, UUID completionId) throws Exception {
        String body = mvc.perform(get("/api/v1/rescue-cases/completions/review-queue?pageSize=100")
                        .with(user(admin.toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.contains(completionId.toString());
    }
}
