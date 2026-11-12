package com.uit.petrescueapi;

import com.jayway.jsonpath.JsonPath;
import com.uit.petrescueapi.application.dto.admin.dashboard.SystemDashboardOverviewDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.DashboardDataPort;
import com.uit.petrescueapi.domain.service.ReputationDomainService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the read endpoints return and how many statements they cost, on real PostGIS with every
 * migration applied. Native spatial SQL does not run on H2, so these cannot be unit tests.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.jakarta.persistence.query.timeout=4000",
        "app.rescue.maintenance-delay-ms=3600000"
})
@AutoConfigureMockMvc
class QueryBehaviorIntegrationTest {

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
    @Autowired EntityManagerFactory emf;
    @Autowired DashboardDataPort dashboard;
    @Autowired ReputationDomainService reputation;
    @MockitoBean CloudStoragePort cloud;
    /** No broker in this test: likes and comments publish counter events that nothing here consumes. */
    @MockitoBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @BeforeEach
    void cloudBuildsUrls() {
        when(cloud.buildUrl(anyString())).thenAnswer(i -> "https://cdn.test/" + i.getArgument(0));
    }

    // ── Rescue case detail ──────────────────────────

    @Test
    void caseDetailTellsTheRescuerWhereTheAnimalIs() throws Exception {
        UUID reporter = person();
        UUID caseId = report(reporter);

        mvc.perform(get("/api/v1/rescue-cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(10.5))
                .andExpect(jsonPath("$.data.longitude").value(106.7))
                .andExpect(jsonPath("$.data.locationText").value("Cầu Thị Nghè"))
                .andExpect(jsonPath("$.data.wardName").value("Phường 1"))
                .andExpect(jsonPath("$.data.provinceName").value("Hồ Chí Minh"));
    }

    @Test
    void caseDetailDoesNotRunOneStatementPerConcern() throws Exception {
        UUID caseId = report(person());
        UUID viewer = person();

        long statements = statementsDuring(() -> call(get("/api/v1/rescue-cases/" + caseId), viewer).andExpect(status().isOk()));

        // detail + images + admin check; the viewer here belongs to no organization
        assertThat(statements).isLessThanOrEqualTo(3);
    }

    @Test
    void caseDetailStillHidesThePhoneFromStrangersButShowsItToClaimers() throws Exception {
        UUID reporter = person();
        UUID claimer = person();
        UUID caseId = report(reporter);
        call(post("/api/v1/rescue-cases/" + caseId + "/claim"), claimer, "{\"safetyAcknowledged\":true}").andExpect(status().isOk());

        call(get("/api/v1/rescue-cases/" + caseId), claimer)
                .andExpect(jsonPath("$.data.contactPhone").value("0900000000"))
                .andExpect(jsonPath("$.data.viewerHasActiveClaim").value(true))
                .andExpect(jsonPath("$.data.activeClaimCount").value(1));
        call(get("/api/v1/rescue-cases/" + caseId), reporter)
                .andExpect(jsonPath("$.data.contactPhone").value("0900000000"))
                .andExpect(jsonPath("$.data.viewerIsReporter").value(true));
        call(get("/api/v1/rescue-cases/" + caseId), person()).andExpect(jsonPath("$.data.contactPhone").doesNotExist());
        mvc.perform(get("/api/v1/rescue-cases/" + caseId)).andExpect(jsonPath("$.data.contactPhone").doesNotExist());
    }

    // ── Rescue case lists ───────────────────────────

    @Test
    void caseListGivesAnImageUrlNotAStoragePublicId() throws Exception {
        UUID reporter = person();
        UUID caseId = report(reporter);
        linkCaseImage(caseId, reporter);

        String body = mvc.perform(get("/api/v1/rescue-cases?pageSize=100")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> urls = JsonPath.read(body, "$.data.content[?(@.caseId=='" + caseId + "')].firstImageUrl");
        assertThat(urls).singleElement().asString().startsWith("https://cdn.test/");
    }

    @Test
    void nearbyListCarriesImageLocationAndDistance() throws Exception {
        UUID reporter = person();
        UUID caseId = report(reporter);
        linkCaseImage(caseId, reporter);

        String body = mvc.perform(get("/api/v1/rescue-cases?lat=10.5&lng=106.7&distance=5000&pageSize=100"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String row = "$.data.content[?(@.caseId=='" + caseId + "')]";
        assertThat(JsonPath.<List<String>>read(body, row + ".firstImageUrl")).singleElement().asString().startsWith("https://cdn.test/");
        assertThat(JsonPath.<List<String>>read(body, row + ".locationText")).containsExactly("Cầu Thị Nghè");
        assertThat(JsonPath.<List<Number>>read(body, row + ".distanceMeters")).singleElement()
                .satisfies(d -> assertThat(d.doubleValue()).isLessThan(1));
    }

    @Test
    void completionListCostDoesNotGrowWithItsRowsAndNamesTheCase() throws Exception {
        UUID admin = admin();
        for (int i = 0; i < 5; i++) {
            UUID submitter = person();
            UUID caseId = report(person());
            call(post("/api/v1/rescue-cases/" + caseId + "/complete"), submitter,
                    "{\"verificationImageIds\":[\"" + permanentMedia(submitter) + "\"],\"confirmRescued\":true,\"outcome\":\"TEMP_HOLDING\"}")
                    .andExpect(status().isCreated());
        }

        long[] statements = new long[1];
        String[] body = new String[1];
        statements[0] = statementsDuring(() -> body[0] = mvc.perform(
                        get("/api/v1/rescue-cases/completions?isResolved=false&pageSize=5").with(user(admin.toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        // page + count + one image lookup for the whole page
        assertThat(statements[0]).isLessThanOrEqualTo(3);
        assertThat(JsonPath.<List<String>>read(body[0], "$.data.content[*].caseCode")).hasSize(5).doesNotContainNull();
        assertThat(JsonPath.<List<List<String>>>read(body[0], "$.data.content[*].verificationImagesUrl"))
                .allSatisfy(images -> assertThat(images).hasSize(1));
    }

    @Test
    void deletedCompletionsDoNotShowInTheList() throws Exception {
        UUID admin = admin();
        UUID submitter = person();
        UUID caseId = report(person());
        String created = call(post("/api/v1/rescue-cases/" + caseId + "/complete"), submitter,
                "{\"verificationImageIds\":[\"" + permanentMedia(submitter) + "\"],\"confirmRescued\":true,\"outcome\":\"TEMP_HOLDING\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String completionId = JsonPath.read(created, "$.data.completionId");
        jdbc.update("UPDATE rescue_case_completions SET is_deleted = TRUE WHERE completion_id = ?", UUID.fromString(completionId));

        mvc.perform(get("/api/v1/rescue-cases/completions?isResolved=false&pageSize=100").with(user(admin.toString()).roles("ADMIN")))
                .andExpect(jsonPath("$.data.content[*].completionId", not(hasItem(completionId))));
    }

    @Test
    void mapShowsOnlyCasesThatStillNeedHelpUnlessAskedOtherwise() throws Exception {
        UUID reporter = person();
        UUID open = report(reporter);
        UUID closed = report(reporter);
        call(patch("/api/v1/rescue-cases/" + closed + "/status"), reporter, "{\"status\":\"CLOSED\",\"reason\":\"Đã xong\"}").andExpect(status().isOk());

        for (String url : List.of("/api/v1/rescue-cases/map/markers",
                "/api/v1/rescue-cases/map/markers?minLat=10&minLng=106&maxLat=11&maxLng=107")) {
            mvc.perform(get(url))
                    .andExpect(jsonPath("$.data[*].caseId", hasItem(open.toString())))
                    .andExpect(jsonPath("$.data[*].caseId", not(hasItem(closed.toString()))));
        }
        mvc.perform(get("/api/v1/rescue-cases/map/markers?status=CLOSED"))
                .andExpect(jsonPath("$.data[*].caseId", hasItem(closed.toString())));
    }

    // ── Dashboard ───────────────────────────────────

    @Test
    void overviewCountsEveryTableOnceAndComparesWithThePreviousMonthEnd() {
        YearMonth month = YearMonth.of(2031, 3);
        YearMonth previous = month.minusMonths(1);
        long pendingBefore = countPendingOrganizationsBefore(month.plusMonths(1));
        long totalBefore = countOrganizationsBefore(month.plusMonths(1));
        long openBefore = countOpenCasesBefore(month.plusMonths(1));

        UUID reporter = person();
        organizationCreatedAt(previous.atDay(10).atStartOfDay(), "ACTIVE");
        organizationCreatedAt(month.atDay(10).atStartOfDay(), "PENDING");
        insertCase(reporter, "REPORTED", previous.atDay(10).atStartOfDay());
        insertCase(reporter, "IN_PROGRESS", month.atDay(10).atStartOfDay());
        insertCase(reporter, "CLOSED", month.atDay(11).atStartOfDay());

        SystemDashboardOverviewDto overview = dashboard.getOverview(month.getMonthValue(), month.getYear());

        assertThat(overview.getKpis().getTotalOrganizations().getValue()).isEqualTo(totalBefore + 2);
        assertThat(overview.getKpis().getPendingOrganizations().getValue()).isEqualTo(pendingBefore + 1);
        assertThat(overview.getKpis().getOpenRescueCases().getValue()).isEqualTo(openBefore + 2);
    }

    @Test
    void topOrganizationsAreNotLimitedToOrganizationsCreatedThatMonth() {
        YearMonth month = YearMonth.of(2032, 5);
        UUID old = organizationCreatedAt(month.minusYears(1).atDay(1).atStartOfDay(), "ACTIVE");
        insertCaseFor(old, person(), month.atDay(2).atStartOfDay());

        var items = dashboard.getTopOrganizations(month.getMonthValue(), month.getYear(), 500).getItems();

        assertThat(items).anySatisfy(item -> {
            assertThat(item.getOrganizationId()).isEqualTo(old);
            assertThat(item.getOpenRescueCases()).isEqualTo(1);
        });
    }

    @Test
    void pendingTasksKeepWorkLeftFromEarlierMonths() {
        YearMonth month = YearMonth.of(2033, 7);
        UUID old = organizationCreatedAt(month.minusMonths(6).atDay(1).atStartOfDay(), "PENDING");

        var items = dashboard.getPendingTasks(month.getMonthValue(), month.getYear()).getItems();

        assertThat(items).anySatisfy(task -> assertThat(task.getTaskId()).isEqualTo(old.toString()));
    }

    // ── Pets ────────────────────────────────────────

    @Test
    void personalOwnerPhoneIsNotPublicButShelterPhoneIs() throws Exception {
        UUID owner = person();
        jdbc.update("UPDATE users SET phone = '0911111111' WHERE user_id = ?", owner);
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        jdbc.update("UPDATE organizations SET phone = '0288888888' WHERE organization_id = ?", shelter);
        UUID personalPet = pet("USER", owner, null);
        UUID shelterPet = pet("ORGANIZATION", shelter, shelter);

        String list = mvc.perform(get("/api/v1/pets?pageSize=100")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(list, "$.data.content[?(@.petId=='" + personalPet + "')].owner.phone")).isEmpty();
        assertThat(JsonPath.<List<String>>read(list, "$.data.content[?(@.petId=='" + shelterPet + "')].owner.phone")).containsExactly("0288888888");

        mvc.perform(get("/api/v1/pets/" + personalPet)).andExpect(jsonPath("$.data.owner.phone").doesNotExist());
        mvc.perform(get("/api/v1/pets/" + shelterPet)).andExpect(jsonPath("$.data.owner.phone").value("0288888888"));
    }

    @Test
    void anOrganizationWithAnEmptyProvinceCodeDoesNotBreakPetEndpoints() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        jdbc.update("UPDATE organizations SET province_code = '', ward_code = '' WHERE organization_id = ?", shelter);
        UUID petId = pet("ORGANIZATION", shelter, shelter);

        mvc.perform(get("/api/v1/pets?status=AVAILABLE&pageSize=100")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/pets/" + petId)).andExpect(status().isOk());
    }

    @Test
    void petDetailReadsItsImagesOnce() throws Exception {
        UUID owner = person();
        UUID petId = pet("USER", owner, null);
        UUID media = permanentMedia(owner);
        jdbc.update("""
                INSERT INTO pet_media (media_id, pet_id, media_file_id, is_primary, created_at)
                VALUES (?, ?, ?, TRUE, NOW())
                """, UUID.randomUUID(), petId, media);

        long statements = statementsDuring(() -> mvc.perform(get("/api/v1/pets/" + petId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryImageUrl", startsWith("https://cdn.test/")))
                .andExpect(jsonPath("$.data.imageUrls[0]", startsWith("https://cdn.test/"))));

        assertThat(statements).isLessThanOrEqualTo(2);
    }

    // ── Organizations ───────────────────────────────

    @Test
    void publicOrganizationMapShowsOnlyActiveOnesByDefault() throws Exception {
        UUID active = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID pending = organizationCreatedAt(LocalDateTime.now(), "PENDING");
        jdbc.update("UPDATE organizations SET location = ST_SetSRID(ST_MakePoint(106.7, 10.5), 4326) WHERE organization_id IN (?, ?)", active, pending);

        for (String url : List.of("/api/v1/organizations/map/markers",
                "/api/v1/organizations/map/markers?minLat=10&minLng=106&maxLat=11&maxLng=107")) {
            mvc.perform(get(url))
                    .andExpect(jsonPath("$.data[*].organizationId", hasItem(active.toString())))
                    .andExpect(jsonPath("$.data[*].organizationId", not(hasItem(pending.toString()))));
        }
    }

    @Test
    void organizationListCarriesTheColumnsItAlreadyReads() throws Exception {
        UUID org = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        jdbc.update("UPDATE organizations SET phone = '0288888888', email = 'a@b.test', street_address = '1 Lê Lợi', image_url = 'https://i.test/a.png' WHERE organization_id = ?", org);

        String body = call(get("/api/v1/organizations?pageSize=100"), admin()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String row = "$.data.content[?(@.organizationId=='" + org + "')]";
        assertThat(JsonPath.<List<String>>read(body, row + ".phone")).containsExactly("0288888888");
        assertThat(JsonPath.<List<String>>read(body, row + ".email")).containsExactly("a@b.test");
        assertThat(JsonPath.<List<String>>read(body, row + ".streetAddress")).containsExactly("1 Lê Lợi");
        assertThat(JsonPath.<List<String>>read(body, row + ".imageUrl")).containsExactly("https://i.test/a.png");
    }

    // ── Adoption ────────────────────────────────────

    @Test
    void applicationDetailGivesReviewersAndApplicantsWhatTheyNeed() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID applicant = person();
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID application = application(petId, applicant, shelter, "REJECTED");
        jdbc.update("UPDATE adoption_applications SET note = 'Có sân vườn', reject_reason = 'Chưa đủ điều kiện' WHERE application_id = ?", application);

        call(get("/api/v1/adoptions/" + application), applicant)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicantId").value(applicant.toString()))
                .andExpect(jsonPath("$.data.applicantUsername").isNotEmpty())
                .andExpect(jsonPath("$.data.organizationId").value(shelter.toString()))
                .andExpect(jsonPath("$.data.organizationName").isNotEmpty())
                .andExpect(jsonPath("$.data.note").value("Có sân vườn"))
                .andExpect(jsonPath("$.data.rejectReason").value("Chưa đủ điều kiện"));
    }

    @Test
    void applicationListShowsThePrimaryImageAndTheApplicant() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID applicant = person();
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        // alphabetically first, but not the primary image
        jdbc.update("INSERT INTO media_files (media_id, uploader_id, public_id, resource_type, status) VALUES (?, ?, 'a-side', 'image', 'PERMANENT')",
                UUID.fromString("00000000-0000-0000-0000-0000000000a1"), applicant);
        jdbc.update("INSERT INTO media_files (media_id, uploader_id, public_id, resource_type, status) VALUES (?, ?, 'z-primary', 'image', 'PERMANENT')",
                UUID.fromString("00000000-0000-0000-0000-0000000000a2"), applicant);
        jdbc.update("INSERT INTO pet_media (media_id, pet_id, media_file_id, is_primary, created_at) VALUES (?, ?, '00000000-0000-0000-0000-0000000000a1', FALSE, NOW())", UUID.randomUUID(), petId);
        jdbc.update("INSERT INTO pet_media (media_id, pet_id, media_file_id, is_primary, created_at) VALUES (?, ?, '00000000-0000-0000-0000-0000000000a2', TRUE, NOW())", UUID.randomUUID(), petId);
        UUID application = application(petId, applicant, shelter, "PENDING");

        String body = mvc.perform(get("/api/v1/adoptions?pageSize=100").with(user(admin().toString()).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String row = "$.data.content[?(@.applicationId=='" + application + "')]";
        assertThat(JsonPath.<List<String>>read(body, row + ".petPrimaryImageUrl")).containsExactly("https://cdn.test/z-primary");
        assertThat(JsonPath.<List<String>>read(body, row + ".applicantUsername")).singleElement().asString().isNotBlank();
    }

    @Test
    void completingAnAdoptionClosesTheOtherOpenApplicationsForThatPet() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID winner = application(petId, person(), shelter, "APPROVED");
        UUID waiting = application(petId, person(), shelter, "PENDING");
        UUID approvedToo = application(petId, person(), shelter, "APPROVED");
        UUID alreadyRejected = application(petId, person(), shelter, "REJECTED");

        call(patch("/api/v1/adoptions/" + winner + "/complete"), owner).andExpect(status().isOk());

        assertThat(applicationStatus(winner)).isEqualTo("COMPLETED");
        assertThat(applicationStatus(waiting)).isEqualTo("CANCELED");
        assertThat(applicationStatus(approvedToo)).isEqualTo("CANCELED");
        assertThat(applicationStatus(alreadyRejected)).isEqualTo("REJECTED");
    }

    // ── External calls ──────────────────────────────

    @Test
    void imagesMoveAfterCommitSoNoTransactionWaitsForCloudStorage() throws Exception {
        AtomicBoolean moved = new AtomicBoolean();
        AtomicBoolean moveRanInTransaction = new AtomicBoolean();
        when(cloud.moveToPermament(anyString(), any(), any())).thenAnswer(i -> {
            moveRanInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            moved.set(true);
            return "pet-rescue/moved/" + UUID.randomUUID();
        });
        UUID submitter = person();
        UUID caseId = report(person());
        UUID media = tempMedia(submitter);

        call(post("/api/v1/rescue-cases/" + caseId + "/complete"), submitter,
                "{\"verificationImageIds\":[\"" + media + "\"],\"confirmRescued\":true,\"outcome\":\"TEMP_HOLDING\"}")
                .andExpect(status().isCreated());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT status FROM media_files WHERE media_id = ?", String.class, media)).isEqualTo("PERMANENT"));
        assertThat(moved).isTrue();
        assertThat(moveRanInTransaction).isFalse();
    }

    @Test
    void aRefusedReportDoesNotTouchCloudStorage() throws Exception {
        UUID submitter = person();
        UUID reporter = person();
        UUID caseId = report(reporter);
        call(patch("/api/v1/rescue-cases/" + caseId + "/status"), reporter, "{\"status\":\"CLOSED\",\"reason\":\"Đã xong\"}").andExpect(status().isOk());
        UUID media = tempMedia(submitter);

        call(post("/api/v1/rescue-cases/" + caseId + "/complete"), submitter,
                "{\"verificationImageIds\":[\"" + media + "\"],\"confirmRescued\":true,\"outcome\":\"TEMP_HOLDING\"}")
                .andExpect(status().isConflict());

        Thread.sleep(500);
        verify(cloud, never()).moveToPermament(org.mockito.ArgumentMatchers.eq("temp/" + media), any(), any());
        assertThat(jdbc.queryForObject("SELECT status FROM media_files WHERE media_id = ?", String.class, media)).isEqualTo("TEMP");
    }

    // ── Reputation, friends, paging ─────────────────

    @Test
    void concurrentReputationChangesAreAllCounted() throws Exception {
        UUID userId = person();
        reputation.adjust(userId, 1, "SEED", "TEST", UUID.randomUUID(), userId);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                futures.add(pool.submit(() -> reputation.adjust(userId, 1, "TEST", "TEST", UUID.randomUUID(), userId)));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT score FROM user_reputations WHERE user_id = ?", Integer.class, userId)).isEqualTo(21);
    }

    @Test
    void cursorFriendListDoesNotCountTheWholeTable() throws Exception {
        UUID me = person();
        for (int i = 0; i < 3; i++) {
            jdbc.update("""
                    INSERT INTO friend_requests (request_id, requester_id, addressee_id, status, created_at, responded_at)
                    VALUES (?, ?, ?, 'ACCEPTED', NOW(), NOW())
                    """, UUID.randomUUID(), me, person());
        }
        String[] body = new String[1];
        long statements = statementsDuring(() -> body[0] = call(get("/api/v1/friends?pageSize=2"), me)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(statements).isEqualTo(1);
        assertThat(JsonPath.<List<Object>>read(body[0], "$.data.items")).hasSize(2);
        assertThat(JsonPath.<Boolean>read(body[0], "$.data.hasMore")).isTrue();
    }

    @Test
    void aQueryThatRunsTooLongIsStopped() {
        try (var em = emf.createEntityManager()) {
            long started = System.nanoTime();
            assertThatThrownBy(() -> em.createNativeQuery("SELECT pg_sleep(15)").getResultList()).isInstanceOf(RuntimeException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
        }
    }

    @Test
    void aHugePageSizeIsCapped() throws Exception {
        mvc.perform(get("/api/v1/rescue-cases?pageSize=100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    void sizeIsAcceptedAsAnAliasOfPageSize() throws Exception {
        mvc.perform(get("/api/v1/rescue-cases?size=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(3));
        mvc.perform(get("/api/v1/rescue-cases?size=3&pageSize=7"))
                .andExpect(jsonPath("$.data.size").value(7));
    }

    // ── Filters ─────────────────────────────────────

    @Test
    void caseListAppliesStatusPrioritySpeciesAndProvince() throws Exception {
        UUID reporter = person();
        String species = "Sp" + UUID.randomUUID().toString().substring(0, 8);
        UUID match = filterCase(reporter, species, "CRITICAL", "REPORTED", "79", "Hồ Chí Minh");
        UUID otherStatus = filterCase(reporter, species, "CRITICAL", "CLOSED", "79", "Hồ Chí Minh");
        UUID otherPriority = filterCase(reporter, species, "LOW", "REPORTED", "79", "Hồ Chí Minh");
        UUID otherProvince = filterCase(reporter, species, "CRITICAL", "REPORTED", "01", "Hà Nội");

        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&species=" + species.toUpperCase()))
                .containsExactlyInAnyOrder(match, otherStatus, otherPriority, otherProvince);
        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&species=" + species + "&status=REPORTED"))
                .containsExactlyInAnyOrder(match, otherPriority, otherProvince);
        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&species=" + species + "&status=REPORTED&status=CLOSED&priority=CRITICAL"))
                .containsExactlyInAnyOrder(match, otherStatus, otherProvince);
        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&species=" + species + "&status=REPORTED&priority=CRITICAL&province=79"))
                .containsExactly(match);
        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&species=" + species + "&province=hà nội"))
                .containsExactly(otherProvince);
    }

    @Test
    void nearbyAndBoundingBoxListsApplyTheSameFilters() throws Exception {
        UUID reporter = person();
        String species = "Sp" + UUID.randomUUID().toString().substring(0, 8);
        UUID open = filterCase(reporter, species, "HIGH", "REPORTED", "79", "Hồ Chí Minh");
        filterCase(reporter, species, "HIGH", "CLOSED", "79", "Hồ Chí Minh");

        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&lat=10.5&lng=106.7&distance=500&species=" + species + "&status=REPORTED"))
                .containsExactly(open);
        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&minLat=10.4&minLng=106.6&maxLat=10.6&maxLng=106.8&species="
                + species + "&status=REPORTED")).containsExactly(open);
        assertThat(caseIds("/api/v1/rescue-cases?pageSize=100&lat=10.5&lng=106.7&distance=500&species=" + species)).hasSize(2);
    }

    @Test
    void petsFilterByTheProvinceOfTheirOrganization() throws Exception {
        String code = "P" + UUID.randomUUID().toString().substring(0, 6);
        UUID here = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        jdbc.update("UPDATE organizations SET province_code = ?, province_name = 'Tỉnh Thử' WHERE organization_id = ?", code, here);
        UUID elsewhere = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID petHere = pet("ORGANIZATION", here, here);
        pet("ORGANIZATION", elsewhere, elsewhere);
        pet("USER", person(), null);

        assertThat(petIds(mvc.perform(get("/api/v1/pets?pageSize=100&province=" + code)))).containsExactly(petHere);
        assertThat(petIds(mvc.perform(get("/api/v1/pets?pageSize=100&availableOnly=true&province=" + code)))).containsExactly(petHere);
        assertThat(petIds(mvc.perform(get("/api/v1/pets?pageSize=100&search=Mực&province=" + code)))).containsExactly(petHere);
        assertThat(petIds(mvc.perform(get("/api/v1/pets?pageSize=100&province=tỉnh thử")))).contains(petHere);
    }

    @Test
    void anyoneCanAskForAvailablePetsButNotForOtherStatuses() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID available = pet("ORGANIZATION", shelter, shelter);
        UUID adopted = pet("ORGANIZATION", shelter, shelter);
        jdbc.update("UPDATE pets SET status = 'ADOPTED' WHERE pet_id = ?", adopted);
        String scope = "/api/v1/pets?pageSize=100&organizationId=" + shelter;

        assertThat(petIds(mvc.perform(get(scope + "&status=AVAILABLE")))).containsExactly(available);
        // a restricted status is dropped for a stranger, as before, rather than honoured
        assertThat(petIds(mvc.perform(get(scope + "&status=ADOPTED")))).containsExactlyInAnyOrder(available, adopted);
        assertThat(petIds(call(get(scope + "&status=ADOPTED"), member(shelter, "STAFF")))).containsExactly(adopted);
    }

    @Test
    void myPetsListsOnlyWhatICurrentlyOwn() throws Exception {
        UUID me = person();
        UUID mine = pet("USER", me, null);
        pet("USER", person(), null);

        assertThat(petIds(call(get("/api/v1/users/me/pets"), me))).containsExactly(mine);
    }

    @Test
    void anotherUsersContactDetailsAreOnlyForThemAndAdmins() throws Exception {
        UUID owner = person();
        jdbc.update("UPDATE users SET phone = '0911111111', street_address = '1 Lê Lợi' WHERE user_id = ?", owner);

        call(get("/api/v1/users/" + owner), person())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.streetAddress").doesNotExist());
        call(get("/api/v1/users/" + owner), owner).andExpect(jsonPath("$.data.phone").value("0911111111"));
        call(get("/api/v1/users/" + owner), admin()).andExpect(jsonPath("$.data.email").exists());
    }

    // ── Posts and rescue cases ──────────────────────

    private static final String CASE_FORM = """
            {"species":"Dog","priority":"HIGH","description":"Bị thương ở chân","latitude":10.5,"longitude":106.7,
             "locationText":"Cầu Thị Nghè","wardName":"Phường 1","provinceName":"Hồ Chí Minh","provinceCode":"79"%s}
            """;

    @Test
    void reportingACaseCanPublishItsPostInTheSameCall() throws Exception {
        UUID reporter = person();

        String body = call(post("/api/v1/rescue-cases"), reporter, CASE_FORM.formatted(",\"shareAsPost\":true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sharedPostId").isNotEmpty())
                .andExpect(jsonPath("$.data.postShareFailed").value(false))
                .andReturn().getResponse().getContentAsString();
        String caseId = JsonPath.read(body, "$.data.caseId");
        String postId = JsonPath.read(body, "$.data.sharedPostId");
        linkCaseImage(UUID.fromString(caseId), reporter);

        // the post is written from the report, labelled as a rescue post, and shows the case as a card
        mvc.perform(get("/api/v1/posts/feed?rescueCaseId=" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].postId").value(postId))
                .andExpect(jsonPath("$.data.items[0].content").value("Cần giúp đỡ: Dog tại Cầu Thị Nghè, Phường 1, Hồ Chí Minh. Bị thương ở chân"))
                .andExpect(jsonPath("$.data.items[0].facets.category").value("RESCUE"))
                .andExpect(jsonPath("$.data.items[0].facets.species").value("Dog"))
                .andExpect(jsonPath("$.data.items[0].facets.needsRescue").value(true))
                .andExpect(jsonPath("$.data.items[0].rescueCase.caseId").value(caseId))
                .andExpect(jsonPath("$.data.items[0].rescueCase.caseCode").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].rescueCase.status").value("REPORTED"))
                .andExpect(jsonPath("$.data.items[0].rescueCase.open").value(true))
                .andExpect(jsonPath("$.data.items[0].rescueCase.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.items[0].rescueCase.locationText").value("Cầu Thị Nghè"))
                .andExpect(jsonPath("$.data.items[0].rescueCase.imageUrl").value(startsWith("https://cdn.test/")));
        mvc.perform(get("/api/v1/posts/feed?category=RESCUE&needsRescue=true&hasOpenCase=true"))
                .andExpect(jsonPath("$.data.items[*].postId").value(hasItem(postId)));
        // a guest reads the feed, a signed-in viewer also learns what they liked
        mvc.perform(get("/api/v1/posts/feed?rescueCaseId=" + caseId)).andExpect(jsonPath("$.data.items[0].likedByMe").doesNotExist());
        call(get("/api/v1/posts/feed?rescueCaseId=" + caseId), reporter).andExpect(jsonPath("$.data.items[0].likedByMe").value(false));
        mvc.perform(get("/api/v1/posts")).andExpect(status().isUnauthorized());

        // the card follows the case, not a copy made when the post was written
        jdbc.update("UPDATE rescue_cases SET status = 'CLOSED' WHERE case_id = ?::uuid", caseId);
        call(get("/api/v1/posts/" + postId), reporter)
                .andExpect(jsonPath("$.data.rescueCase.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.rescueCase.open").value(false));
    }

    @Test
    void aReportWithoutSharingPublishesNothing() throws Exception {
        String body = call(post("/api/v1/rescue-cases"), person(), CASE_FORM.formatted(""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sharedPostId").doesNotExist())
                .andExpect(jsonPath("$.data.postShareFailed").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/v1/posts/feed?rescueCaseId=" + JsonPath.<String>read(body, "$.data.caseId")))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void anyoneSharesAnOpenCaseOnceAndCanUnlinkTheirPost() throws Exception {
        UUID reporter = person();
        UUID sharer = person();
        UUID caseId = report(reporter);
        String shareBody = "{\"content\":\"Ai ở gần giúp bé với\",\"rescueCaseId\":\"" + caseId + "\"}";

        String created = call(post("/api/v1/posts"), sharer, shareBody).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String postId = JsonPath.read(created, "$.data.postId");
        call(post("/api/v1/posts"), sharer, shareBody)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POST_RESCUE_CASE_ALREADY_SHARED"));
        call(post("/api/v1/posts"), reporter, shareBody).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/posts/feed?rescueCaseId=" + caseId)).andExpect(jsonPath("$.data.items.length()").value(2));

        call(put("/api/v1/posts/" + postId), sharer, "{\"removeRescueCase\":true}").andExpect(status().isOk());
        mvc.perform(get("/api/v1/posts/feed?rescueCaseId=" + caseId)).andExpect(jsonPath("$.data.items.length()").value(1));
        call(get("/api/v1/posts/" + postId), sharer).andExpect(jsonPath("$.data.rescueCase").doesNotExist());

        jdbc.update("UPDATE rescue_cases SET status = 'RESCUED' WHERE case_id = ?", caseId);
        call(put("/api/v1/posts/" + postId), sharer, "{\"rescueCaseId\":\"" + caseId + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POST_RESCUE_CASE_CLOSED"));
    }

    @Test
    void aSharedPostLinkOpensForGuestsWithItsComments() throws Exception {
        UUID author = person();
        UUID reader = person();
        String created = call(post("/api/v1/posts"), author, "{\"content\":\"Bé mèo nhà mình hôm nay\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String postId = JsonPath.read(created, "$.data.postId");
        call(post("/api/v1/posts/" + postId + "/comments"), reader, "{\"content\":\"Dễ thương quá\"}").andExpect(status().isCreated());
        call(post("/api/v1/posts/" + postId + "/like"), reader).andExpect(status().isOk());

        // a guest reads the post and its comments, without any "liked by me"
        mvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Bé mèo nhà mình hôm nay"))
                .andExpect(jsonPath("$.data.rescueCase").doesNotExist())
                .andExpect(jsonPath("$.data.likedByMe").doesNotExist());
        mvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].content").value("Dễ thương quá"));
        call(get("/api/v1/posts/" + postId), reader).andExpect(jsonPath("$.data.likedByMe").value(true));
        call(get("/api/v1/posts/" + postId), author).andExpect(jsonPath("$.data.likedByMe").value(false));

        // writing still needs an account, and so do the list and the analysis
        mvc.perform(post("/api/v1/posts/" + postId + "/comments").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/posts/" + postId + "/like")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/posts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/posts/" + postId + "/analysis")).andExpect(status().isUnauthorized());
    }

    @Test
    void myRescueCasesListsOnlyWhatIReported() throws Exception {
        UUID me = person();
        UUID open = report(me);
        UUID closed = report(me);
        jdbc.update("UPDATE rescue_cases SET status = 'CLOSED' WHERE case_id = ?", closed);
        report(person());

        String all = call(get("/api/v1/users/me/rescue-cases"), me).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(all, "$.data.content[*].caseId"))
                .containsExactlyInAnyOrder(open.toString(), closed.toString());
        String stillOpen = call(get("/api/v1/users/me/rescue-cases?status=REPORTED&status=IN_PROGRESS"), me)
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(stillOpen, "$.data.content[*].caseId")).containsExactly(open.toString());
    }

    // ── Adoption notifications ──────────────────────

    @Test
    void adoptionStepsReachTheApplicantAndTheOrganization() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID staff = member(shelter, "STAFF");
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID applicant = person();
        UUID rival = person();
        UUID rivalApplication = application(petId, rival, shelter, "PENDING");

        // no signature: it is optional now
        String body = call(post("/api/v1/adoptions"), applicant, """
                {"petId":"%s","experience":"Đã nuôi chó mười năm","housingCondition":"%s","liveCondition":"Nhà có sân"}
                """.formatted(petId, com.uit.petrescueapi.domain.valueobject.HousingCondition.values()[0].name()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String applicationId = JsonPath.read(body, "$.data.applicationId");
        assertThat(notificationTypes(owner)).containsExactly("ADOPTION_SUBMITTED");
        assertThat(notificationTypes(staff)).containsExactly("ADOPTION_SUBMITTED");

        call(patch("/api/v1/adoptions/" + applicationId + "/approve"), staff, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ADOPTION_READY_AT_REQUIRED"));
        call(patch("/api/v1/adoptions/" + applicationId + "/approve"), staff, "{\"readyAt\":\"2030-01-01T10:00:00\"}")
                .andExpect(status().isOk());
        call(patch("/api/v1/adoptions/" + applicationId + "/complete"), owner, "{}").andExpect(status().isOk());

        assertThat(notificationTypes(applicant)).containsExactly("ADOPTION_APPROVED", "ADOPTION_COMPLETED");
        assertThat(applicationStatus(rivalApplication)).isEqualTo("CANCELED");
        assertThat(notificationTypes(rival)).containsExactly("ADOPTION_CANCELED");
    }

    // ── Helpers ─────────────────────────────────────

    private List<String> notificationTypes(UUID userId) {
        return jdbc.queryForList("SELECT type FROM notifications WHERE user_id = ? ORDER BY created_at, type", String.class, userId);
    }

    private UUID filterCase(UUID reporter, String species, String priority, String caseStatus, String provinceCode, String provinceName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO rescue_cases (case_id, case_code, species, priority, status, reported_by, reported_at, created_at,
                                          province_code, province_name, location)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?, ST_SetSRID(ST_MakePoint(106.7, 10.5), 4326))
                """, id, id.toString().substring(0, 12), species, priority, caseStatus, reporter, provinceCode, provinceName);
        return id;
    }

    private List<UUID> caseIds(String url) throws Exception {
        String body = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.<List<String>>read(body, "$.data.content[*].caseId").stream().map(UUID::fromString).toList();
    }

    private List<UUID> petIds(ResultActions result) throws Exception {
        String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.<List<String>>read(body, "$.data.content[*].petId").stream().map(UUID::fromString).toList();
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private long statementsDuring(Action action) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        // async work started by earlier requests (notification delivery, media moves) counts here too
        long[] last = {-1};
        await().pollInterval(Duration.ofMillis(300)).atMost(Duration.ofSeconds(10)).until(() -> {
            long now = stats.getPrepareStatementCount();
            boolean settled = now == last[0];
            last[0] = now;
            return settled;
        });
        stats.clear();
        action.run();
        return stats.getPrepareStatementCount();
    }

    private ResultActions call(MockHttpServletRequestBuilder request, UUID caller) throws Exception {
        return mvc.perform(request.with(user(caller.toString()).roles("USER")));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, UUID caller, String body) throws Exception {
        return mvc.perform(request.with(user(caller.toString()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID report(UUID reporter) throws Exception {
        String body = call(post("/api/v1/rescue-cases"), reporter, """
                {"species":"Dog","priority":"HIGH","description":"Bị thương","latitude":10.5,"longitude":106.7,
                 "locationText":"Cầu Thị Nghè","wardName":"Phường 1","provinceName":"Hồ Chí Minh",
                 "provinceCode":"79","contactPhone":"0900000000"}
                """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.data.caseId"));
    }

    private UUID person() {
        UUID id = UUID.randomUUID();
        String tag = id.toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO users (user_id, username, email, status, email_verified, user_code)
                VALUES (?, ?, ?, 'ACTIVE', TRUE, ?)
                """, id, "u-" + tag, tag + "@petrescue.test", tag);
        grant(id, "USER");
        return id;
    }

    private void grant(UUID userId, String role) {
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, role_id FROM roles WHERE code = ?", userId, role);
    }

    private UUID admin() {
        UUID id = person();
        grant(id, "ADMIN");
        return id;
    }

    private UUID member(UUID organizationId, String role) {
        UUID id = person();
        jdbc.update("""
                INSERT INTO organization_members (organization_id, user_id, role, joined_at, status)
                VALUES (?, ?, ?, NOW(), 'ACTIVE')
                """, organizationId, id, role);
        return id;
    }

    private UUID permanentMedia(UUID uploader) {
        return media(uploader, "PERMANENT");
    }

    private UUID tempMedia(UUID uploader) {
        return media(uploader, "TEMP");
    }

    private UUID media(UUID uploader, String mediaStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO media_files (media_id, uploader_id, public_id, resource_type, status)
                VALUES (?, ?, ?, 'image', ?)
                """, id, uploader, (mediaStatus.equals("TEMP") ? "temp/" : "test/") + id, mediaStatus);
        return id;
    }

    private void linkCaseImage(UUID caseId, UUID uploader) {
        jdbc.update("INSERT INTO rescue_media (case_id, media_id) VALUES (?, ?)", caseId, permanentMedia(uploader));
    }

    private UUID organizationCreatedAt(LocalDateTime createdAt, String organizationStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO organizations (organization_id, name, type, province_code, status, organization_code, created_at)
                VALUES (?, ?, 'SHELTER', '79', ?, ?, ?)
                """, id, "Org " + id, organizationStatus, id.toString().substring(0, 12), Timestamp.valueOf(createdAt));
        return id;
    }

    private void insertCase(UUID reporter, String caseStatus, LocalDateTime createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO rescue_cases (case_id, case_code, species, priority, status, reported_by, reported_at, created_at)
                VALUES (?, ?, 'Dog', 'HIGH', ?, ?, ?, ?)
                """, id, id.toString().substring(0, 12), caseStatus, reporter, Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt));
    }

    private void insertCaseFor(UUID organizationId, UUID reporter, LocalDateTime createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO rescue_cases (case_id, case_code, species, priority, status, reported_by, organization_id, reported_at, created_at)
                VALUES (?, ?, 'Dog', 'HIGH', 'IN_PROGRESS', ?, ?, ?, ?)
                """, id, id.toString().substring(0, 12), reporter, organizationId, Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt));
    }

    private long countOrganizationsBefore(YearMonth end) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM organizations WHERE is_deleted = FALSE AND created_at < ?", Long.class,
                Timestamp.valueOf(end.atDay(1).atStartOfDay()));
    }

    private long countPendingOrganizationsBefore(YearMonth end) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM organizations WHERE is_deleted = FALSE AND status = 'PENDING' AND created_at < ?", Long.class,
                Timestamp.valueOf(end.atDay(1).atStartOfDay()));
    }

    private long countOpenCasesBefore(YearMonth end) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM rescue_cases WHERE is_deleted = FALSE AND status IN ('REPORTED','IN_PROGRESS') AND created_at < ?", Long.class,
                Timestamp.valueOf(end.atDay(1).atStartOfDay()));
    }

    private UUID pet(String ownerType, UUID ownerId, UUID shelterId) {
        UUID id = UUID.randomUUID();
        UUID species = jdbc.queryForObject("SELECT species_id FROM pet_species LIMIT 1", UUID.class);
        jdbc.update("""
                INSERT INTO pets (pet_id, pet_code, name, species_id, status, shelter_id, gender)
                VALUES (?, ?, 'Mực', ?, 'AVAILABLE', ?, 'MALE')
                """, id, id.toString().substring(0, 12), species, shelterId);
        jdbc.update("INSERT INTO pets_current_owner (pet_id, owner_type, owner_id) VALUES (?, ?, ?)", id, ownerType, ownerId);
        return id;
    }

    private UUID application(UUID petId, UUID applicant, UUID organizationId, String applicationStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO adoption_applications (application_id, adoption_code, pet_id, applicant_id, organization_id, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, id.toString().substring(0, 12), petId, applicant, organizationId, applicationStatus);
        return id;
    }

    private String applicationStatus(UUID id) {
        return jdbc.queryForObject("SELECT status FROM adoption_applications WHERE application_id = ?", String.class, id);
    }
}
