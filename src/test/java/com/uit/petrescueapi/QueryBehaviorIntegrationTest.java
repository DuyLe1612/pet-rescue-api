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
import static org.hamcrest.Matchers.hasSize;
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
    @Autowired com.uit.petrescueapi.application.port.command.FollowupCommandPort followupCommands;
    @Autowired com.uit.petrescueapi.domain.service.FollowupDomainService followups;
    @Autowired EntityManagerFactory emf;
    @Autowired DashboardDataPort dashboard;
    @Autowired ReputationDomainService reputation;
    @Autowired com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository triage;
    @MockitoBean CloudStoragePort cloud;
    /** There is no broker in this test, and every message it is given is refused. */
    @MockitoBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @BeforeEach
    void cloudBuildsUrls() {
        org.mockito.Mockito.doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
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
    void someoneAboutToReportSeesOpenRecentCasesOfTheSameAnimalNearby() throws Exception {
        UUID reporter = person();
        double lat = 11.25, lng = 107.35;
        UUID sameDog = reportAt(reporter, "Dog", lat, lng);
        UUID unknownSpecies = reportAt(reporter, null, lat + 0.0005, lng);
        UUID cat = reportAt(reporter, "Cat", lat, lng);
        UUID farAway = reportAt(reporter, "Dog", lat + 0.05, lng);
        UUID lastWeek = reportAt(reporter, "Dog", lat, lng);
        jdbc.update("UPDATE rescue_cases SET reported_at = now() - interval '5 days' WHERE case_id = ?", lastWeek);
        UUID closed = reportAt(reporter, "Dog", lat, lng);
        jdbc.update("UPDATE rescue_cases SET status = 'CLOSED' WHERE case_id = ?", closed);

        String body = mvc.perform(get("/api/v1/rescue-cases/possible-duplicates?lat=" + lat + "&lng=" + lng + "&species=dog"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> ids = JsonPath.read(body, "$.data[*].caseId");
        assertThat(ids).containsExactly(sameDog.toString(), unknownSpecies.toString());
        assertThat(ids).doesNotContain(cat.toString(), farAway.toString(), lastWeek.toString(), closed.toString());

        String wider = mvc.perform(get("/api/v1/rescue-cases/possible-duplicates?lat=" + lat + "&lng=" + lng + "&hours=168"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(wider, "$.data[*].caseId"))
                .contains(sameDog.toString(), cat.toString(), lastWeek.toString())
                .doesNotContain(farAway.toString(), closed.toString());
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

    @Test
    void adoptingRecordsHowTheNewOwnerGotThePetAndWhichApplicationDidIt() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID adopter = person();
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID application = application(petId, adopter, shelter, "APPROVED");

        call(patch("/api/v1/adoptions/" + application + "/complete"), owner).andExpect(status().isOk());

        var current = jdbc.queryForMap("SELECT owner_type, owner_id, acquisition_type, adoption_application_id FROM pets_current_owner WHERE pet_id = ?", petId);
        assertThat(current.get("owner_type")).isEqualTo("USER");
        assertThat(current.get("owner_id")).isEqualTo(adopter);
        assertThat(current.get("acquisition_type")).isEqualTo("ADOPTED");
        assertThat(current.get("adoption_application_id")).isEqualTo(application);
        assertThat(jdbc.queryForObject("SELECT acquisition_type FROM pet_ownerships WHERE pet_id = ? AND to_time IS NULL", String.class, petId))
                .isEqualTo("ADOPTED");
    }

    @Test
    void completingAnAdoptionSchedulesFourMilestonesOnceAndCancelsOnlyTheOpenOnes() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID application = application(petId, person(), shelter, "APPROVED");

        call(patch("/api/v1/adoptions/" + application + "/complete"), owner).andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT milestone FROM adoption_followups WHERE application_id = ? ORDER BY due_at", String.class, application))
                .containsExactly("DAY_2", "WEEK_2", "MONTH_3", "MONTH_6");
        assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM adoption_applications WHERE application_id = ?", Boolean.class, application)).isTrue();
        // due two days after completion, as configured
        assertThat(jdbc.queryForObject("""
                SELECT f.due_at = a.completed_at + INTERVAL '2 days'
                FROM adoption_followups f JOIN adoption_applications a ON a.application_id = f.application_id
                WHERE f.application_id = ? AND f.milestone = 'DAY_2'""", Boolean.class, application)).isTrue();

        followups.scheduleFor(application, LocalDateTime.now());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM adoption_followups WHERE application_id = ?", Integer.class, application)).isEqualTo(4);

        jdbc.update("UPDATE adoption_followups SET status = 'RESPONDED' WHERE application_id = ? AND milestone = 'DAY_2'", application);
        followups.cancelOpen(application);
        assertThat(jdbc.queryForList("SELECT status FROM adoption_followups WHERE application_id = ? ORDER BY due_at", String.class, application))
                .containsExactly("RESPONDED", "CANCELLED", "CANCELLED", "CANCELLED");
    }

    // ── Follow-up after an adoption ─────────────────

    /** An adoption completed just now; its milestones are made due by the tests that need them. */
    private UUID adoptedApplication(UUID shelter, UUID adopter) throws Exception {
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID application = application(petId, adopter, shelter, "APPROVED");
        call(patch("/api/v1/adoptions/" + application + "/complete"), member(shelter, "OWNER")).andExpect(status().isOk());
        return application;
    }

    private void makeDue(UUID application, String milestone) {
        jdbc.update("UPDATE adoption_followups SET due_at = now() - INTERVAL '1 minute' WHERE application_id = ? AND milestone = ?", application, milestone);
    }

    private static final String ANSWERS_FINE = "{\"stillHasPet\":true,\"wellbeing\":\"NORMAL\",\"fear\":\"NONE\",\"incident\":\"NONE\",\"support\":\"NONE\"}";

    @Test
    void aDueMilestoneIsSentOnceAndItsLinkWorksOnceWithoutSigningIn() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        makeDue(application, "DAY_2");
        long reputationLogs = jdbc.queryForObject("SELECT COUNT(*) FROM reputation_logs WHERE user_id = ?", Long.class, adopter);

        var sent = followupCommands.dispatchDue(20);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).followup().milestone()).isEqualTo("DAY_2");
        assertThat(followupCommands.dispatchDue(20)).as("a second pass sends nothing more").isEmpty();
        // only the hash is stored: the token itself is never in the database
        String token = sent.get(0).token();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM adoption_followups WHERE application_id = ? AND milestone = 'DAY_2'", String.class, application))
                .isNotEqualTo(token).hasSize(64);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'FOLLOWUP_DUE'", Integer.class, adopter)).isEqualTo(1);

        mvc.perform(get("/api/v1/followups/respond?token=" + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.petName").value("Mực"))
                .andExpect(jsonPath("$.data.milestone").value("DAY_2"))
                .andExpect(jsonPath("$.data.questions", hasSize(5)));
        mvc.perform(post("/api/v1/followups/respond").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"answers\":" + ANSWERS_FINE + "}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status || '/' || severity FROM adoption_followups WHERE application_id = ? AND milestone = 'DAY_2'", String.class, application))
                .isEqualTo("RESPONDED/GREEN");

        // used, so it opens nothing and answers nothing; a made-up token is no different
        mvc.perform(get("/api/v1/followups/respond?token=" + token)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/followups/respond").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"answers\":" + ANSWERS_FINE + "}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/followups/respond?token=not-a-real-token")).andExpect(status().isBadRequest());
        // answering is not rewarded or punished
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reputation_logs WHERE user_id = ?", Long.class, adopter)).isEqualTo(reputationLogs);
    }

    @Test
    void anExpiredLinkOpensNothingAndWhenSeveralAreDueOnlyTheNewestIsAsked() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID application = adoptedApplication(shelter, person());
        makeDue(application, "DAY_2");
        jdbc.update("UPDATE adoption_followups SET due_at = now() - INTERVAL '1 day' WHERE application_id = ? AND milestone = 'DAY_2'", application);
        makeDue(application, "WEEK_2");

        var sent = followupCommands.dispatchDue(20);

        assertThat(sent).extracting(d -> d.followup().milestone()).containsExactly("WEEK_2");
        assertThat(jdbc.queryForObject("SELECT status FROM adoption_followups WHERE application_id = ? AND milestone = 'DAY_2'", String.class, application)).isEqualTo("EXPIRED");

        jdbc.update("UPDATE adoption_followups SET expires_at = now() - INTERVAL '1 minute' WHERE application_id = ? AND milestone = 'WEEK_2'", application);
        mvc.perform(get("/api/v1/followups/respond?token=" + sent.get(0).token())).andExpect(status().isBadRequest());
        assertThat(followupCommands.expire()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM adoption_followups WHERE application_id = ? AND milestone = 'WEEK_2'", String.class, application)).isEqualTo("EXPIRED");
    }

    @Test
    void aMilestoneNobodyAnsweredIsRemindedOnceAfterTwoDays() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        makeDue(application, "DAY_2");
        followupCommands.dispatchDue(20);

        assertThat(followupCommands.remind()).as("too soon").isZero();
        jdbc.update("UPDATE adoption_followups SET sent_at = now() - INTERVAL '3 days' WHERE application_id = ? AND milestone = 'DAY_2'", application);
        assertThat(followupCommands.remind()).isEqualTo(1);
        assertThat(followupCommands.remind()).as("only once").isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'FOLLOWUP_REMINDER'", Integer.class, adopter)).isEqualTo(1);
    }

    @Test
    void theAdopterAnswersSignedInAndOnlyTheAdopterTheOrganizationAndAdminsReadTheTimeline() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        makeDue(application, "DAY_2");
        followupCommands.dispatchDue(20);
        UUID followup = jdbc.queryForObject("SELECT followup_id FROM adoption_followups WHERE application_id = ? AND milestone = 'DAY_2'", UUID.class, application);

        call(post("/api/v1/followups/" + followup + "/answers"), person(), ANSWERS_FINE).andExpect(status().isNotFound());
        call(post("/api/v1/followups/" + followup + "/answers"), adopter,
                "{\"stillHasPet\":true,\"wellbeing\":\"WORRIED\",\"fear\":\"SOME\",\"incident\":\"NONE\",\"support\":\"NONE\",\"note\":\"Bé hơi sợ\"}")
                .andExpect(status().isOk());

        String url = "/api/v1/adoptions/" + application + "/followups";
        call(get(url), adopter).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].milestone").value("DAY_2"))
                .andExpect(jsonPath("$.data[0].severity").value("YELLOW"))
                .andExpect(jsonPath("$.data[0].answers.note").value("Bé hơi sợ"));
        call(get(url), member(shelter, "STAFF")).andExpect(status().isOk());
        call(get(url), admin()).andExpect(status().isOk());
        // another organization's staff, and a bystander, read nothing
        call(get(url), member(organizationCreatedAt(LocalDateTime.now(), "ACTIVE"), "OWNER")).andExpect(status().isForbidden());
        call(get(url), person()).andExpect(status().isForbidden());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    /** Sends the milestone and answers it through the signed-in endpoint; returns the follow-up id. */
    private UUID answered(UUID application, UUID adopter, String milestone, String answers) throws Exception {
        makeDue(application, milestone);
        followupCommands.dispatchDue(20);
        UUID followup = jdbc.queryForObject("SELECT followup_id FROM adoption_followups WHERE application_id = ? AND milestone = ?", UUID.class, application, milestone);
        call(post("/api/v1/followups/" + followup + "/answers"), adopter, answers).andExpect(status().isOk());
        return followup;
    }

    @Test
    void worryingAnswersBecomeWorkForTheOrganizationAndOnlyRedOnesNotifyStaffAtOnce() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);

        answered(application, adopter, "DAY_2", ANSWERS_FINE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM followup_alerts WHERE application_id = ?", Integer.class, application)).as("green makes no work").isZero();

        answered(application, adopter, "WEEK_2",
                "{\"stillHasPet\":true,\"wellbeing\":\"WORRIED\",\"fear\":\"NONE\",\"incident\":\"NONE\",\"support\":\"NONE\"}");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'FOLLOWUP_ALERT'", Integer.class, staff)).as("yellow waits in the queue").isZero();

        answered(application, adopter, "MONTH_3",
                "{\"stillHasPet\":true,\"wellbeing\":\"NORMAL\",\"fear\":\"NONE\",\"incident\":\"NONE\",\"support\":\"CONSIDERING_RETURN\"}");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'FOLLOWUP_ALERT'", Integer.class, staff)).isEqualTo(1);

        // red on top, with every reason; the queue is the organization's own
        call(get("/api/v1/organizations/me/followup-alerts"), staff).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].severity").value("RED"))
                .andExpect(jsonPath("$.data[0].reasons[0]").value("CONSIDERING_RETURN"))
                .andExpect(jsonPath("$.data[0].petName").value("Mực"))
                .andExpect(jsonPath("$.data[1].severity").value("YELLOW"));
        call(get("/api/v1/organizations/me/followup-alerts?status=RESOLVED"), staff).andExpect(jsonPath("$.data", hasSize(0)));
        call(get("/api/v1/organizations/me/followup-alerts"), member(organizationCreatedAt(LocalDateTime.now(), "ACTIVE"), "STAFF"))
                .andExpect(jsonPath("$.data", hasSize(0)));
        call(get("/api/v1/organizations/me/followup-alerts"), adopter).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/organizations/me/followup-alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void anUnansweredFirstQuestionAsksTheOrganizationToCallButALaterOneDoesNot() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        for (String milestone : List.of("DAY_2", "WEEK_2")) {
            makeDue(application, milestone);
            followupCommands.dispatchDue(20);
            jdbc.update("UPDATE adoption_followups SET status = 'SENT', expires_at = now() - INTERVAL '1 minute' WHERE application_id = ? AND milestone = ?", application, milestone);
        }

        assertThat(followupCommands.expire()).isEqualTo(2);

        assertThat(jdbc.queryForList("SELECT severity || ':' || reason_codes FROM followup_alerts WHERE application_id = ?", String.class, application))
                .containsExactly("YELLOW:NO_RESPONSE");
    }

    @Test
    void staffTakeAndCloseAnAlertAndAnotherOrganizationCannot() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        answered(application, adopter, "DAY_2",
                "{\"stillHasPet\":true,\"wellbeing\":\"NORMAL\",\"fear\":\"MUCH\",\"incident\":\"NONE\",\"support\":\"NONE\"}");
        UUID alert = jdbc.queryForObject("SELECT alert_id FROM followup_alerts WHERE application_id = ?", UUID.class, application);
        String url = "/api/v1/followup-alerts/" + alert;

        call(patch(url), member(organizationCreatedAt(LocalDateTime.now(), "ACTIVE"), "OWNER"), "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isForbidden());
        call(patch(url), adopter, "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isForbidden());
        call(patch(url), staff, "{\"status\":\"DONE\"}").andExpect(status().isBadRequest());
        call(patch(url), staff, "{\"status\":\"RESOLVED\"}").andExpect(status().isBadRequest());

        call(patch(url), staff, "{\"status\":\"IN_PROGRESS\",\"note\":\"Đã gọi, hẹn chiều nay\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assignedTo").value(staff.toString()))
                .andExpect(jsonPath("$.data.firstActionAt").isNotEmpty())
                .andExpect(jsonPath("$.data.resolvedAt").doesNotExist());
        call(patch(url), staff, "{\"status\":\"RESOLVED\",\"resolution\":\"ADVISED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolution").value("ADVISED"))
                .andExpect(jsonPath("$.data.note").value("Đã gọi, hẹn chiều nay"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());
    }

    private long reputationLogCount(UUID user) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reputation_logs WHERE user_id = ?", Long.class, user);
    }

    @Test
    void anAdopterWhoCannotKeepThePetAsksToReturnItAndTheOrganizationBringsItBackWithoutCostingReputation() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        UUID petId = jdbc.queryForObject("SELECT pet_id FROM adoption_applications WHERE application_id = ?", UUID.class, application);
        long reputationBefore = reputationLogCount(adopter);
        String ask = "/api/v1/adoptions/" + application + "/return-request";

        call(post(ask), person(), "{\"reasonCode\":\"HOUSING_MOVING\"}").andExpect(status().isNotFound());
        call(post(ask), adopter, "{\"reasonCode\":\"NOPE\"}").andExpect(status().isBadRequest());
        call(post(ask), adopter, "{\"reasonCode\":\"HOUSING_MOVING\",\"note\":\"Chuyển nhà ra nước ngoài\"}").andExpect(status().isOk());
        call(post(ask), adopter, "{\"reasonCode\":\"HOUSING_MOVING\"}").andExpect(status().isOk());

        call(get("/api/v1/organizations/me/followup-alerts"), staff).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].severity").value("RED"))
                .andExpect(jsonPath("$.data[0].reasons[0]").value("RETURN_REQUESTED"))
                .andExpect(jsonPath("$.data[0].reasons[1]").value("HOUSING_MOVING"))
                .andExpect(jsonPath("$.data[0].note").value("Chuyển nhà ra nước ngoài"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'FOLLOWUP_ALERT'", Integer.class, staff)).isEqualTo(1);

        String outcome = "/api/v1/adoptions/" + application + "/outcome";
        String returned = "{\"outcomeType\":\"RETURNED_BY_ADOPTER\",\"reasonCode\":\"HOUSING_MOVING\"}";
        call(post(outcome), adopter, returned).andExpect(status().isForbidden());
        call(post(outcome), member(organizationCreatedAt(LocalDateTime.now(), "ACTIVE"), "OWNER"), returned).andExpect(status().isForbidden());
        call(post(outcome), staff, "{\"outcomeType\":\"SOLD\"}").andExpect(status().isBadRequest());
        call(post(outcome), staff, returned).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcomeType").value("RETURNED_BY_ADOPTER"))
                .andExpect(jsonPath("$.data.daysSinceCompleted").value(0));

        var owner = jdbc.queryForMap("SELECT owner_type, owner_id, acquisition_type FROM pets_current_owner WHERE pet_id = ?", petId);
        assertThat(owner.get("owner_type")).isEqualTo("ORGANIZATION");
        assertThat(owner.get("owner_id")).isEqualTo(shelter);
        assertThat(owner.get("acquisition_type")).isEqualTo("ORG_INTAKE");
        assertThat(jdbc.queryForObject("SELECT status FROM pets WHERE pet_id = ?", String.class, petId)).isEqualTo("FOSTERING");
        assertThat(jdbc.queryForObject("SELECT initiated_by FROM adoption_outcomes WHERE application_id = ?", UUID.class, application)).isEqualTo(adopter);
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM adoption_followups WHERE application_id = ?", String.class, application)).containsExactly("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM followup_alerts WHERE application_id = ?", String.class, application)).isEqualTo("RESOLVED");
        assertThat(reputationLogCount(adopter)).as("giving a pet back costs nothing").isEqualTo(reputationBefore + 0);

        // it ended: nothing more to record, ask or send
        call(post(outcome), staff, returned).andExpect(status().isConflict());
        call(post(ask), adopter, "{\"reasonCode\":\"OTHER\"}").andExpect(status().isConflict());
        assertThat(followupCommands.dispatchDue(20)).isEmpty();
    }

    @Test
    void aSupportRequestIsYellowWorkOnceAndTurningFollowUpOffStopsTheQuestions() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        String support = "/api/v1/adoptions/" + application + "/support-request";

        call(post(support), adopter, "{\"note\":\"Bé không chịu ăn\"}").andExpect(status().isOk());
        call(post(support), adopter, "{}").andExpect(status().isOk());
        call(get("/api/v1/organizations/me/followup-alerts"), staff).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].severity").value("YELLOW"))
                .andExpect(jsonPath("$.data[0].reasons[0]").value("SUPPORT_REQUESTED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'FOLLOWUP_ALERT'", Integer.class, staff)).as("yellow does not page").isZero();

        String preference = "/api/v1/adoptions/" + application + "/followup-preference";
        call(patch(preference), person(), "{\"enabled\":false}").andExpect(status().isNotFound());
        call(patch(preference), adopter, "{\"enabled\":false}").andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM adoption_followups WHERE application_id = ?", String.class, application)).containsExactly("CANCELLED");
        makeDue(application, "DAY_2");
        assertThat(followupCommands.dispatchDue(20)).isEmpty();

        call(patch(preference), adopter, "{\"enabled\":true}").andExpect(status().isOk());
        // WEEK_2, MONTH_3 and MONTH_6 are still ahead; DAY_2 was already due, so it stays cancelled
        assertThat(jdbc.queryForList("SELECT milestone || ':' || status FROM adoption_followups WHERE application_id = ? ORDER BY due_at", String.class, application))
                .containsExactly("DAY_2:CANCELLED", "WEEK_2:SCHEDULED", "MONTH_3:SCHEDULED", "MONTH_6:SCHEDULED");
    }

    @Test
    void reclaimingForAViolationIsRecordedAsAnOutcomeAndStillCostsReputation() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID adopter = person();
        UUID application = adoptedApplication(shelter, adopter);
        UUID proof = tempMedia(owner);
        when(cloud.moveToPermament(anyString(), any(), any())).thenReturn("pet-rescue/reclaims/proof");

        call(post("/api/v1/adoptions/" + application + "/reclaim"), owner,
                "{\"reason\":\"Bị bỏ đói nhiều ngày liền\",\"proofId\":\"" + proof + "\"}").andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT outcome_type FROM adoption_outcomes WHERE application_id = ?", String.class, application)).isEqualTo("RECLAIMED");
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM adoption_followups WHERE application_id = ?", String.class, application)).containsExactly("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(delta), 0) FROM reputation_logs WHERE user_id = ? AND reason = 'PET_RECLAIMED'", Integer.class, adopter)).isEqualTo(-300);
    }

    @Test
    void theAdopterProfileFormIsPrefilledFromThePetsThePersonKeepsAlive() throws Exception {
        UUID me = person();
        String url = "/api/v1/users/me/adopter-profile/suggestion";
        call(get(url), me).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasDog").value(false))
                .andExpect(jsonPath("$.data.hasCat").value(false))
                .andExpect(jsonPath("$.data.basedOnPetIds", hasSize(0)));

        UUID dog = pet("USER", me, null);
        jdbc.update("UPDATE pets SET species_id = (SELECT species_id FROM pet_species WHERE name = 'Dog'), status = 'ADOPTED' WHERE pet_id = ?", dog);
        UUID deadCat = pet("USER", me, null);
        jdbc.update("UPDATE pets SET species_id = (SELECT species_id FROM pet_species WHERE name = 'Cat'), status = 'DECEASED' WHERE pet_id = ?", deadCat);
        UUID someoneElsesCat = pet("USER", person(), null);
        jdbc.update("UPDATE pets SET species_id = (SELECT species_id FROM pet_species WHERE name = 'Cat') WHERE pet_id = ?", someoneElsesCat);

        call(get(url), me).andExpect(jsonPath("$.data.hasDog").value(true))
                .andExpect(jsonPath("$.data.hasCat").value(false))
                .andExpect(jsonPath("$.data.basedOnPetIds", hasSize(1)))
                .andExpect(jsonPath("$.data.basedOnPetIds[0]").value(dog.toString()));
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    private long metric(String json, String path) {
        List<Number> found = JsonPath.read(json, path);
        return found.isEmpty() ? 0 : found.get(0).longValue();
    }

    @Test
    void followUpNumbersCountNotAnsweringOnItsOwnAndAreForAdminsOnly() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        String url = "/api/v1/admin/followups/metrics";
        call(get(url), staff).andExpect(status().isForbidden());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        String before = mvc.perform(get(url).with(user(admin().toString()).roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID answeringAdopter = person();
        UUID answering = adoptedApplication(shelter, answeringAdopter);
        answered(answering, answeringAdopter, "DAY_2", ANSWERS_FINE);
        UUID silent = adoptedApplication(shelter, person());
        makeDue(silent, "DAY_2");
        followupCommands.dispatchDue(20);
        UUID leaving = adoptedApplication(shelter, person());
        call(post("/api/v1/adoptions/" + leaving + "/outcome"), staff, "{\"outcomeType\":\"RETURNED_BY_ADOPTER\",\"reasonCode\":\"TIME\"}").andExpect(status().isOk());

        String after = mvc.perform(get(url).with(user(admin().toString()).roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String day2 = "$.data.milestones[?(@.milestone=='DAY_2')].";
        assertThat(metric(after, day2 + "asked") - metric(before, day2 + "asked")).isEqualTo(2);
        assertThat(metric(after, day2 + "responded") - metric(before, day2 + "responded")).isEqualTo(1);
        assertThat(metric(after, day2 + "unanswered") - metric(before, day2 + "unanswered")).as("silence is its own number").isEqualTo(1);
        assertThat(metric(after, day2 + "stillHasPet") - metric(before, day2 + "stillHasPet")).isEqualTo(1);
        String returned = "$.data.outcomes[?(@.outcomeType=='RETURNED_BY_ADOPTER' && @.reasonCode=='TIME')].count";
        assertThat(metric(after, returned) - metric(before, returned)).isEqualTo(1);
    }

    @Test
    void aPetAPersonRegistersIsSelfRegisteredAndOneAnOrganizationCreatesIsOrgIntake() throws Exception {
        UUID species = jdbc.queryForObject("SELECT species_id FROM pet_species LIMIT 1", UUID.class);
        UUID person = person();
        String body = "{\"name\":\"Bơ\",\"speciesId\":\"" + species + "\",\"gender\":\"MALE\"}";

        String created = call(post("/api/v1/pets"), person, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID mine = UUID.fromString(JsonPath.read(created, "$.data.petId"));

        assertThat(jdbc.queryForObject("SELECT acquisition_type FROM pets_current_owner WHERE pet_id = ?", String.class, mine)).isEqualTo("SELF_REGISTERED");
        assertThat(jdbc.queryForObject("SELECT acquisition_type FROM pet_ownerships WHERE pet_id = ?", String.class, mine)).isEqualTo("SELF_REGISTERED");
        assertThat(jdbc.queryForObject("SELECT adoption_application_id FROM pets_current_owner WHERE pet_id = ?", UUID.class, mine)).isNull();

        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "OWNER");
        String orgBody = "{\"name\":\"Mướp\",\"speciesId\":\"" + species + "\",\"gender\":\"FEMALE\",\"shelterId\":\"" + shelter + "\"}";
        String org = call(post("/api/v1/pets"), staff, orgBody).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID theirs = UUID.fromString(JsonPath.read(org, "$.data.petId"));
        assertThat(jdbc.queryForObject("SELECT acquisition_type FROM pets_current_owner WHERE pet_id = ?", String.class, theirs)).isEqualTo("ORG_INTAKE");
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

    // ── Filters, sorting and names ──────────────────

    private static final List<String> CASE_LISTS = List.of(
            "/api/v1/rescue-cases?pageSize=100",
            "/api/v1/rescue-cases?pageSize=100&lat=10.5&lng=106.7&distance=5000",
            "/api/v1/rescue-cases?pageSize=100&minLat=10&minLng=106&maxLat=11&maxLng=107");

    @Test
    void everyCaseListAppliesStatusPriorityAndSpecies() throws Exception {
        UUID reporter = person();
        String openDog = caseAt(reporter, "Dog", "HIGH", "REPORTED").toString();
        String closedDog = caseAt(reporter, "Dog", "HIGH", "CLOSED").toString();
        String openCat = caseAt(reporter, "Cat", "LOW", "REPORTED").toString();

        for (String url : CASE_LISTS) {
            mvc.perform(get(url + "&status=REPORTED&status=IN_PROGRESS")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].caseId", hasItem(openDog)))
                    .andExpect(jsonPath("$.data.content[*].caseId", hasItem(openCat)))
                    .andExpect(jsonPath("$.data.content[*].caseId", not(hasItem(closedDog))));
            mvc.perform(get(url + "&species=cat")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].caseId", hasItem(openCat)))
                    .andExpect(jsonPath("$.data.content[*].caseId", not(hasItem(openDog))));
            mvc.perform(get(url + "&priority=LOW")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].caseId", hasItem(openCat)))
                    .andExpect(jsonPath("$.data.content[*].caseId", not(hasItem(openDog))));
            // no status asked for: every status, as before
            mvc.perform(get(url + "&species=Dog&priority=HIGH")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].caseId", hasItem(closedDog)));
        }
    }

    @Test
    void everyCaseListRowCanBeDrawnOnAMap() throws Exception {
        String species = "Sp" + UUID.randomUUID().toString().substring(0, 8);
        UUID caseId = caseAt(person(), species, "HIGH", "REPORTED");
        jdbc.update("UPDATE rescue_cases SET needs_help = TRUE WHERE case_id = ?", caseId);

        for (String url : CASE_LISTS) {
            String body = mvc.perform(get(url + "&species=" + species)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String row = "$.data.content[?(@.caseId=='" + caseId + "')]";
            assertThat(JsonPath.<List<Double>>read(body, row + ".latitude")).containsExactly(10.5);
            assertThat(JsonPath.<List<Double>>read(body, row + ".longitude")).containsExactly(106.7);
            assertThat(JsonPath.<List<String>>read(body, row + ".wardName")).containsExactly("Phường 1");
            assertThat(JsonPath.<List<String>>read(body, row + ".provinceName")).containsExactly("Hồ Chí Minh");
            assertThat(JsonPath.<List<Boolean>>read(body, row + ".needsHelp")).containsExactly(true);
        }
        mvc.perform(get("/api/v1/rescue-cases/map/markers?minLat=10&minLng=106&maxLat=11&maxLng=107"))
                .andExpect(jsonPath("$.data[?(@.caseId=='" + caseId + "')].needsHelp", hasItem(true)));
    }

    @Test
    void anIncompleteOrImpossibleAreaIsRefusedInsteadOfIgnored() throws Exception {
        for (String url : List.of(
                "/api/v1/rescue-cases?minLat=10&minLng=106",
                "/api/v1/rescue-cases?minLat=11&minLng=106&maxLat=10&maxLng=107",
                "/api/v1/rescue-cases?lat=10.5",
                "/api/v1/rescue-cases?lat=95&lng=106.7",
                "/api/v1/rescue-cases?lat=10.5&lng=106.7&distance=-1",
                "/api/v1/rescue-cases/map/markers?minLat=10&minLng=106&maxLat=11",
                "/api/v1/organizations/map/markers?maxLng=107",
                "/api/v1/organizations/map/bounding-box?minLat=11&minLng=106&maxLat=10&maxLng=107")) {
            mvc.perform(get(url)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void anUnknownSortColumnIsRefusedAndAKnownOneWorks() throws Exception {
        UUID admin = admin();
        mvc.perform(get("/api/v1/rescue-cases?sortBy=nope")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/rescue-cases?minLat=10&minLng=106&maxLat=11&maxLng=107&sortBy=nope")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/organizations/map/bounding-box?minLat=10&minLng=106&maxLat=11&maxLng=107&sortBy=nope"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/adoptions/reclaims?sortBy=nope").with(user(admin.toString()).roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/rescue-cases?sortBy=caseCode&sortOrder=asc")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rescue-cases?minLat=10&minLng=106&maxLat=11&maxLng=107&sortBy=createdAt")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/organizations/map/bounding-box?minLat=10&minLng=106&maxLat=11&maxLng=107&sortBy=name&sortOrder=asc"))
                .andExpect(status().isOk());
    }

    @Test
    void prioritySortPutsTheMostUrgentFirst() throws Exception {
        UUID reporter = person();
        String species = "Sp" + UUID.randomUUID().toString().substring(0, 8);
        String low = caseAt(reporter, species, "LOW", "REPORTED").toString();
        String critical = caseAt(reporter, species, "CRITICAL", "REPORTED").toString();
        String normal = caseAt(reporter, species, "NORMAL", "REPORTED").toString();
        String high = caseAt(reporter, species, "HIGH", "REPORTED").toString();

        String body = mvc.perform(get("/api/v1/rescue-cases?sortBy=priority&sortOrder=desc&species=" + species))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$.data.content[*].caseId")).containsExactly(critical, high, normal, low);
    }

    @Test
    void caseDetailNamesTheOrganizationAndThePet() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID caseId = report(person());
        jdbc.update("UPDATE rescue_cases SET organization_id = ?, pet_id = ? WHERE case_id = ?", shelter, petId, caseId);

        mvc.perform(get("/api/v1/rescue-cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("Org " + shelter))
                .andExpect(jsonPath("$.data.petName").value("Mực"));
    }

    @Test
    void reclaimLogNamesThePetTheOrganizationAndThePeople() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID adopter = person();
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        UUID application = application(petId, adopter, shelter, "COMPLETED");
        UUID reclaim = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reclaim_logs (reclaim_id, application_id, pet_id, organization_id, user_id, reclaimed_by, reason, proof_id, reputation_delta)
                VALUES (?, ?, ?, ?, ?, ?, 'Bỏ bê', ?, -300)
                """, reclaim, application, petId, shelter, adopter, owner, permanentMedia(owner));

        for (String url : List.of("/api/v1/adoptions/reclaims?pageSize=100", "/api/v1/adoptions/reclaims?pageSize=100&organizationId=" + shelter)) {
            String body = mvc.perform(get(url).with(user(admin().toString()).roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String row = "$.data.content[?(@.reclaimId=='" + reclaim + "')]";
            assertThat(JsonPath.<List<String>>read(body, row + ".petName")).containsExactly("Mực");
            assertThat(JsonPath.<List<String>>read(body, row + ".organizationName")).containsExactly("Org " + shelter);
            assertThat(JsonPath.<List<String>>read(body, row + ".username")).singleElement().asString().startsWith("u-");
            assertThat(JsonPath.<List<String>>read(body, row + ".reclaimedByUsername")).singleElement().asString().startsWith("u-");
            assertThat(JsonPath.<List<String>>read(body, row + ".reason")).containsExactly("Bỏ bê");
            assertThat(JsonPath.<List<String>>read(body, row + ".reclaimedAt")).singleElement().isNotNull();
        }
    }

    @Test
    void applicationListNamesTheOrganization() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID applicant = person();
        UUID application = application(pet("ORGANIZATION", shelter, shelter), applicant, shelter, "PENDING");

        String body = call(get("/api/v1/adoptions/user/" + applicant + "?pageSize=100"), applicant)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String row = "$.data.content[?(@.applicationId=='" + application + "')]";
        assertThat(JsonPath.<List<String>>read(body, row + ".organizationId")).containsExactly(shelter.toString());
        assertThat(JsonPath.<List<String>>read(body, row + ".organizationName")).containsExactly("Org " + shelter);
        assertThat(JsonPath.<List<String>>read(body, row + ".applicantId")).containsExactly(applicant.toString());
    }

    @Test
    void organizationMarkerCarriesWhatAPopupShows() throws Exception {
        UUID org = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        jdbc.update("""
                UPDATE organizations SET phone = '0288888888', image_url = 'https://i.test/a.png', street_address = '1 Lê Lợi',
                       location = ST_SetSRID(ST_MakePoint(106.7, 10.5), 4326) WHERE organization_id = ?
                """, org);

        for (String url : List.of("/api/v1/organizations/map/markers",
                "/api/v1/organizations/map/markers?minLat=10&minLng=106&maxLat=11&maxLng=107")) {
            String body = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String row = "$.data[?(@.organizationId=='" + org + "')]";
            assertThat(JsonPath.<List<String>>read(body, row + ".phone")).containsExactly("0288888888");
            assertThat(JsonPath.<List<String>>read(body, row + ".imageUrl")).containsExactly("https://i.test/a.png");
            assertThat(JsonPath.<List<String>>read(body, row + ".streetAddress")).containsExactly("1 Lê Lợi");
            assertThat(JsonPath.<List<String>>read(body, row + ".organizationCode")).containsExactly(org.toString().substring(0, 12));
        }
    }

    @Test
    void availablePetsCanBeSearchedByName() throws Exception {
        UUID owner = person();
        String name = "Pet" + UUID.randomUUID().toString().substring(0, 8);
        String wanted = pet("USER", owner, null).toString();
        String other = pet("USER", owner, null).toString();
        jdbc.update("UPDATE pets SET name = ? WHERE pet_id = ?", name, UUID.fromString(wanted));

        mvc.perform(get("/api/v1/pets?availableOnly=true&pageSize=100&search=" + name)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].petId", hasItem(wanted)))
                .andExpect(jsonPath("$.data.content[*].petId", not(hasItem(other))));
    }

    @Test
    void rescueQueueRowSaysWhereAndShowsAPicture() throws Exception {
        UUID reporter = person();
        UUID caseId = caseAt(reporter, "Dog", "CRITICAL", "REPORTED");
        jdbc.update("UPDATE rescue_cases SET needs_help = TRUE, reported_at = NOW() - INTERVAL '10 years' WHERE case_id = ?", caseId);
        linkCaseImage(caseId, reporter);

        String body = call(get("/api/v1/organizations/me/rescue-queue?pageSize=100"), admin())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String row = "$.data.content[?(@.caseId=='" + caseId + "')]";
        assertThat(JsonPath.<List<String>>read(body, row + ".locationText")).containsExactly("Cầu Thị Nghè");
        assertThat(JsonPath.<List<String>>read(body, row + ".firstImageUrl")).singleElement().asString().startsWith("https://cdn.test/");
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
    void myPetsShowHowIGotEachOneAndWhereAnAdoptedOneCameFrom() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID owner = member(shelter, "OWNER");
        UUID me = person();
        UUID mine = pet("USER", me, null);
        jdbc.update("UPDATE pets_current_owner SET acquisition_type = 'SELF_REGISTERED' WHERE pet_id = ?", mine);
        UUID adopted = pet("ORGANIZATION", shelter, shelter);
        UUID application = application(adopted, me, shelter, "APPROVED");
        call(patch("/api/v1/adoptions/" + application + "/complete"), owner).andExpect(status().isOk());

        String all = call(get("/api/v1/users/me/pets"), me).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(all, "$.data.content[?(@.petId=='" + mine + "')].acquisitionType")).containsExactly("SELF_REGISTERED");
        assertThat(JsonPath.<List<String>>read(all, "$.data.content[?(@.petId=='" + adopted + "')].acquisitionType")).containsExactly("ADOPTED");
        assertThat(JsonPath.<List<String>>read(all, "$.data.content[?(@.petId=='" + adopted + "')].adoptionApplicationId")).containsExactly(application.toString());
        assertThat(JsonPath.<List<String>>read(all, "$.data.content[?(@.petId=='" + adopted + "')].sourceOrganization.organizationId")).containsExactly(shelter.toString());
        assertThat(JsonPath.<List<Object>>read(all, "$.data.content[?(@.petId=='" + mine + "')].sourceOrganization")).isEmpty();

        assertThat(petIds(call(get("/api/v1/users/me/pets?type=ADOPTED"), me))).containsExactly(adopted);
        assertThat(petIds(call(get("/api/v1/users/me/pets?type=SELF_REGISTERED"), me))).containsExactly(mine);
        assertThat(petIds(call(get("/api/v1/users/me/pets?sortBy=name&sortOrder=asc"), me))).containsExactlyInAnyOrder(mine, adopted);
        call(get("/api/v1/users/me/pets?type=NOPE"), me).andExpect(status().isBadRequest());
    }

    @Test
    void aPetAPersonRegistersIsPrivateUntilTheyShowIt() throws Exception {
        UUID species = jdbc.queryForObject("SELECT species_id FROM pet_species LIMIT 1", UUID.class);
        UUID owner = person();
        UUID stranger = person();
        String created = call(post("/api/v1/pets"), owner, "{\"name\":\"Cỏ\",\"speciesId\":\"" + species + "\",\"gender\":\"MALE\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(created, "$.data.petId"));
        String url = "/api/v1/pets/" + id;
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID orgPet = pet("ORGANIZATION", shelter, shelter);

        call(get(url), owner).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.acquisitionType").value("SELF_REGISTERED"));
        call(get(url), admin()).andExpect(status().isOk());
        call(get(url), stranger).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isNotFound());
        assertThat(petIds(mvc.perform(get("/api/v1/pets?pageSize=100")))).contains(orgPet).doesNotContain(id);
        assertThat(petIds(call(get("/api/v1/pets?pageSize=100"), stranger))).doesNotContain(id);
        assertThat(petIds(call(get("/api/v1/users/me/pets"), owner))).containsExactly(id);

        // only whoever manages the pet decides who can see it
        call(patch(url + "/visibility?visibility=PUBLIC"), stranger).andExpect(status().isForbidden());
        mvc.perform(patch(url + "/visibility?visibility=PUBLIC")).andExpect(status().isUnauthorized());
        call(patch(url + "/visibility?visibility=PUBLIC"), owner).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));

        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
        assertThat(petIds(mvc.perform(get("/api/v1/pets?pageSize=100")))).contains(id);

        call(patch(url + "/visibility?visibility=PRIVATE"), owner).andExpect(status().isOk());
        mvc.perform(get(url)).andExpect(status().isNotFound());
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

    // ── Time on the wire ────────────────────────────

    @Test
    void everyDateTimeLeavesTheApiAsUtcWithZ() throws Exception {
        UUID reporter = person();
        LocalDateTime before = LocalDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(5);
        UUID caseId = report(reporter);

        String detail = mvc.perform(get("/api/v1/rescue-cases/" + caseId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String reportedAt = JsonPath.read(detail, "$.data.reportedAt");
        assertThat(reportedAt).endsWith("Z");
        // the instant is right, not shifted by the host's zone
        assertThat(java.time.Instant.parse(reportedAt))
                .isBetween(before.toInstant(java.time.ZoneOffset.UTC), java.time.Instant.now().plusSeconds(5));
        assertThat(JsonPath.<String>read(detail, "$.timestamp")).endsWith("Z");

        String nearby = mvc.perform(get("/api/v1/rescue-cases?lat=10.5&lng=106.7&distance=500&pageSize=100"))
                .andReturn().getResponse().getContentAsString();
        // the list queries convert in SQL, the detail in Java: both must give the same instant
        assertThat(JsonPath.<List<String>>read(nearby, "$.data.content[?(@.caseId=='" + caseId + "')].reportedAt"))
                .containsExactly(reportedAt);
    }

    @Test
    void aCursorSentBackWithZWalksTheWholeFeed() throws Exception {
        UUID author = person();
        for (int i = 0; i < 3; i++) {
            call(post("/api/v1/posts"), author, "{\"content\":\"tz cursor " + i + "\"}").andExpect(status().isCreated());
        }

        String first = mvc.perform(get("/api/v1/posts/feed?pageSize=2")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(first, "$.data.nextCursor");
        String cursorId = JsonPath.read(first, "$.data.nextCursorId");
        assertThat(cursor).endsWith("Z");

        String second = mvc.perform(get("/api/v1/posts/feed").param("pageSize", "2")
                        .param("cursor", cursor).param("cursorId", cursorId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> firstIds = JsonPath.read(first, "$.data.items[*].postId");
        List<String> secondIds = JsonPath.read(second, "$.data.items[*].postId");
        assertThat(secondIds).isNotEmpty().doesNotContainAnyElementsOf(firstIds);

        // an offset other than Z is the same instant
        String shifted = java.time.Instant.parse(cursor).atOffset(java.time.ZoneOffset.ofHours(7)).toString();
        String sameSecond = mvc.perform(get("/api/v1/posts/feed").param("pageSize", "2")
                        .param("cursor", shifted).param("cursorId", cursorId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(sameSecond, "$.data.items[*].postId")).isEqualTo(secondIds);
    }

    @Test
    void theDashboardCountsMonthsAsPeopleInVietnamDo() {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID petId = pet("ORGANIZATION", shelter, shelter);
        int year = 2031;
        // 31 Jan 18:00 UTC is already 1 Feb 01:00 in Vietnam
        UUID earlyFebruary = application(petId, person(), shelter, "PENDING");
        jdbc.update("UPDATE adoption_applications SET created_at = '2031-01-31T18:00:00Z' WHERE application_id = ?", earlyFebruary);
        // 31 Dec 18:00 UTC of the year before is already 1 Jan in Vietnam
        UUID newYear = application(petId, person(), shelter, "PENDING");
        jdbc.update("UPDATE adoption_applications SET created_at = '2030-12-31T18:00:00Z' WHERE application_id = ?", newYear);

        var trend = dashboard.getAdoptionTrend(year);
        String months = String.valueOf(trend);
        assertThat(trend.getItems().stream().filter(m -> m.getMonth() == 1).mapToLong(m -> m.getPending()).sum()).isEqualTo(1);
        assertThat(trend.getItems().stream().filter(m -> m.getMonth() == 2).mapToLong(m -> m.getPending()).sum())
                .as(months).isEqualTo(1);
    }

    // ── AI triage store ─────────────────────────────

    private UUID triageCase(String caseStatus, String reportedAgo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO rescue_cases (case_id, case_code, species, priority, status, reported_by, reported_at, created_at)
                VALUES (?, ?, 'Dog', 'NORMAL', ?, ?, NOW() - CAST(? AS INTERVAL), NOW())
                """, id, id.toString().substring(0, 12), caseStatus, person(), reportedAgo);
        return id;
    }

    @Test
    void oneWorkerTakesATriageAtATimeAndItStopsAfterTheLastAttempt() {
        UUID caseId = triageCase("REPORTED", "10 minutes");
        LocalDateTime stale = LocalDateTime.now().minusMinutes(5);

        assertThat(triage.claim(caseId, 3, stale)).isTrue();
        assertThat(triage.claim(caseId, 3, stale)).as("held by a live worker").isFalse();
        assertThat(triage.findByCaseId(caseId)).get().satisfies(t -> {
            assertThat(t.getStatus()).isEqualTo(com.uit.petrescueapi.domain.valueobject.TriageStatus.PENDING);
            assertThat(t.getAttempts()).isEqualTo(1);
        });

        // a failure releases the claim, and each new claim is one more attempt
        triage.markFailed(caseId, "api error 429", "{\"cat", 120L);
        assertThat(triage.findByCaseId(caseId)).get().satisfies(t -> {
            assertThat(t.getStatus()).isEqualTo(com.uit.petrescueapi.domain.valueobject.TriageStatus.FAILED);
            assertThat(t.getFailure()).isEqualTo("api error 429");
            assertThat(t.getRawOutput()).isEqualTo("{\"cat");
        });
        assertThat(triage.claim(caseId, 3, stale)).isTrue();
        triage.markFailed(caseId, "api error 429", null, 80L);
        assertThat(triage.claim(caseId, 3, stale)).isTrue();
        triage.markFailed(caseId, "api error 429", null, 80L);

        assertThat(triage.claim(caseId, 3, stale)).as("out of attempts").isFalse();
        assertThat(triage.findByCaseId(caseId)).get().extracting(t -> t.getAttempts()).isEqualTo(3);
    }

    @Test
    void aClaimLeftByADeadWorkerCanBeTakenOver() {
        UUID caseId = triageCase("REPORTED", "10 minutes");
        assertThat(triage.claim(caseId, 3, LocalDateTime.now().minusMinutes(5))).isTrue();

        jdbc.update("UPDATE rescue_case_triage SET claimed_at = NOW() - INTERVAL '10 minutes' WHERE case_id = ?", caseId);

        assertThat(triage.claim(caseId, 3, LocalDateTime.now().minusMinutes(5))).isTrue();
        assertThat(triage.findByCaseId(caseId)).get().extracting(t -> t.getAttempts()).isEqualTo(2);
    }

    @Test
    void aFinishedTriageKeepsWhatWasFoundAndIsNeverClaimedAgain() {
        UUID caseId = triageCase("REPORTED", "10 minutes");
        triage.claim(caseId, 3, LocalDateTime.now().minusMinutes(5));

        triage.markAnalyzed(com.uit.petrescueapi.domain.entity.RescueCaseTriage.builder()
                .caseId(caseId).model("openrouter/openai/gpt-oss-120b").promptVersion(1)
                .aiUrgency(com.uit.petrescueapi.domain.valueobject.RescuePriority.CRITICAL).needsRescue(true)
                .conditionSummary("Bleeding from the hind leg").species("Dog").district("Quận 7").province("TP. Hồ Chí Minh")
                .missingFields(List.of("PHOTO", "DESCRIPTION")).rawOutput("{\"category\":\"RESCUE\"}")
                .latencyMs(2100L).inputTokens(940L).outputTokens(480L).build());

        assertThat(triage.findByCaseId(caseId)).get().satisfies(t -> {
            assertThat(t.isAnalyzed()).isTrue();
            assertThat(t.getAiUrgency()).isEqualTo(com.uit.petrescueapi.domain.valueobject.RescuePriority.CRITICAL);
            assertThat(t.getConditionSummary()).isEqualTo("Bleeding from the hind leg");
            assertThat(t.getSpecies()).isEqualTo("Dog");
            assertThat(t.getMissingFields()).containsExactly("PHOTO", "DESCRIPTION");
            assertThat(t.getRawOutput()).isEqualTo("{\"category\":\"RESCUE\"}");
            assertThat(t.getAnalyzedAt()).isNotNull();
            assertThat(t.getFailure()).isNull();
        });
        assertThat(triage.claim(caseId, 3, LocalDateTime.now().minusMinutes(5))).isFalse();
    }

    @Test
    void theRetryPassFindsOnlyOpenRecentCasesThatStillNeedATriage() {
        UUID lostEvent = triageCase("REPORTED", "10 minutes");          // no row at all
        UUID tooNew = triageCase("REPORTED", "10 seconds");             // its own event is still on the way
        UUID failedOnce = triageCase("IN_PROGRESS", "10 minutes");
        UUID outOfAttempts = triageCase("REPORTED", "10 minutes");
        UUID analysed = triageCase("REPORTED", "10 minutes");
        UUID closed = triageCase("CLOSED", "10 minutes");
        UUID tooOld = triageCase("REPORTED", "3 days");

        triage.claim(failedOnce, 3, LocalDateTime.now().minusMinutes(5));
        triage.markFailed(failedOnce, "api error 502", null, 50L);
        for (int i = 0; i < 3; i++) {
            triage.claim(outOfAttempts, 3, LocalDateTime.now().minusMinutes(5));
            triage.markFailed(outOfAttempts, "api error 502", null, 50L);
        }
        triage.claim(analysed, 3, LocalDateTime.now().minusMinutes(5));
        triage.markAnalyzed(com.uit.petrescueapi.domain.entity.RescueCaseTriage.builder().caseId(analysed).build());
        jdbc.update("UPDATE rescue_case_triage SET updated_at = NOW() - INTERVAL '10 minutes' WHERE case_id IN (?, ?, ?)",
                failedOnce, outOfAttempts, analysed);

        List<UUID> found = triage.findRetryable(3, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusDays(1), 1000);

        assertThat(found).contains(lostEvent, failedOnce)
                .doesNotContain(tooNew, outOfAttempts, analysed, closed, tooOld);
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

    // ── Matching ────────────────────────────────────

    private static final String GOOD_HOME = """
            {"housingType":"HOUSE","buildingRule":"ALLOWED","hoursAwayPerDay":4,"activityLevel":3,
             "monthlyBudget":2000000,"adjustmentWeeksExpected":6,"experienceLevel":1,"consent":true%s}
            """;
    private static final String EASY_PET = """
            {"friendlyPeople":5,"friendlyChildren":5,"fearStrangers":1,"fearNoise":1,"energy":3,"goodWithDogs":5,
             "goodWithCats":5,"aloneMinutes":480,"size":"SMALL","noiseLevel":1,"monthlyCost":500000,
             "careLevel":"LOW","sourceRole":"FOSTER"}
            """;
    private static final String HARD_PET = """
            {"friendlyPeople":2,"fearStrangers":5,"energy":5,"goodWithCats":2,"aloneMinutes":30,"size":"LARGE",
             "noiseLevel":5,"monthlyCost":9000000,"careLevel":"HIGH","sourceRole":"FOSTER"}
            """;

    @Test
    void adopterProfileNeedsConsentAndCountsItsVersions() throws Exception {
        UUID me = person();

        call(get("/api/v1/users/me/adopter-profile"), me).andExpect(status().isNotFound());
        call(put("/api/v1/users/me/adopter-profile"), me, "{\"housingType\":\"HOUSE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ADOPTER_PROFILE_CONSENT_REQUIRED"));
        call(put("/api/v1/users/me/adopter-profile"), me, "{\"consent\":true}").andExpect(status().isBadRequest());

        call(put("/api/v1/users/me/adopter-profile"), me, GOOD_HOME.formatted(",\"hasCat\":true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.buildingRule").value("ALLOWED"));
        call(put("/api/v1/users/me/adopter-profile"), me, GOOD_HOME.formatted(""))
                .andExpect(jsonPath("$.data.version").value(2));
        call(get("/api/v1/users/me/adopter-profile"), me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.housingType").value("HOUSE"))
                .andExpect(jsonPath("$.data.hasCat").value(false))
                .andExpect(jsonPath("$.data.consent").value(true));
    }

    @Test
    void whoeverManagesAPetRecordsItsTraitsAndAnyoneReadsThem() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        UUID petId = dog(shelter);

        mvc.perform(get("/api/v1/pets/" + petId + "/traits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.petId").value(petId.toString()))
                .andExpect(jsonPath("$.data.energy").doesNotExist());
        call(put("/api/v1/pets/" + petId + "/traits"), person(), EASY_PET).andExpect(status().isForbidden());
        call(put("/api/v1/pets/" + petId + "/traits"), staff, "{\"energy\":9}").andExpect(status().isBadRequest());
        call(put("/api/v1/pets/" + petId + "/traits"), staff, EASY_PET)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceRole").value("FOSTER"));

        mvc.perform(get("/api/v1/pets/" + petId + "/traits")).andExpect(jsonPath("$.data.energy").value(3));
        mvc.perform(get("/api/v1/pets/" + UUID.randomUUID() + "/traits")).andExpect(status().isNotFound());
    }

    @Test
    void aMatchExplainsItselfAndKeepsTheNumberFromTheAdopter() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID petId = dog(shelter);
        call(put("/api/v1/pets/" + petId + "/traits"), member(shelter, "STAFF"), EASY_PET).andExpect(status().isOk());
        UUID me = person();

        mvc.perform(get("/api/v1/pets/" + petId + "/match")).andExpect(status().isUnauthorized());
        call(get("/api/v1/pets/" + petId + "/match"), me)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ADOPTER_PROFILE_REQUIRED"));

        call(put("/api/v1/users/me/adopter-profile"), me, GOOD_HOME.formatted("")).andExpect(status().isOk());
        call(get("/api/v1/pets/" + petId + "/match"), me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("HIGH"))
                .andExpect(jsonPath("$.data.certainty").value("HIGH"))
                .andExpect(jsonPath("$.data.score").doesNotExist())
                .andExpect(jsonPath("$.data.reasons.length()").value(5))
                .andExpect(jsonPath("$.data.reasons[0].code").value("LIFESTYLE"))
                .andExpect(jsonPath("$.data.reasons[0].text").isNotEmpty())
                .andExpect(jsonPath("$.data.warnings.length()").value(0));

        call(put("/api/v1/users/me/adopter-profile"), me, GOOD_HOME.formatted(",\"allergyDog\":true")).andExpect(status().isOk());
        call(get("/api/v1/pets/" + petId + "/match"), me)
                .andExpect(jsonPath("$.data.level").value("NOT_SUITABLE"))
                .andExpect(jsonPath("$.data.warnings[0].code").value("ALLERGY"))
                .andExpect(jsonPath("$.data.warnings[0].text").isNotEmpty());
    }

    @Test
    void sortByMatchPutsTheBestPetFirstAndWarnedPetsLast() throws Exception {
        String province = "M" + UUID.randomUUID().toString().substring(0, 6);
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        jdbc.update("UPDATE organizations SET province_code = ? WHERE organization_id = ?", province, shelter);
        UUID staff = member(shelter, "STAFF");
        UUID warned = dog(shelter);
        UUID hard = dog(shelter);
        UUID undescribed = dog(shelter);
        UUID easy = dog(shelter);
        UUID notAvailable = dog(shelter);
        jdbc.update("UPDATE pets SET status = 'FOSTERING' WHERE pet_id = ?", notAvailable);
        call(put("/api/v1/pets/" + easy + "/traits"), staff, EASY_PET).andExpect(status().isOk());
        call(put("/api/v1/pets/" + hard + "/traits"), staff, HARD_PET).andExpect(status().isOk());
        call(put("/api/v1/pets/" + warned + "/traits"), staff, "{\"goodWithCats\":1}").andExpect(status().isOk());
        UUID me = person();
        String url = "/api/v1/pets?sort=match&province=" + province;

        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        call(get(url), me).andExpect(status().isUnprocessableEntity());
        call(put("/api/v1/users/me/adopter-profile"), me, GOOD_HOME.formatted(",\"hasCat\":true")).andExpect(status().isOk());

        String body = call(get(url), me).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.content[0].match.level").value("HIGH"))
                .andExpect(jsonPath("$.data.content[0].match.score").doesNotExist())
                .andExpect(jsonPath("$.data.content[3].match.level").value("NOT_SUITABLE"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$.data.content[*].petId"))
                .containsExactly(easy.toString(), undescribed.toString(), hard.toString(), warned.toString());

        // paging walks the same order
        String second = call(get(url + "&page=1&pageSize=2"), me).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(second, "$.data.content[*].petId")).containsExactly(hard.toString(), warned.toString());
    }

    @Test
    void anApplicationFreezesItsMatchAndAWarningOnlyNeedsAcknowledging() throws Exception {
        UUID shelter = organizationCreatedAt(LocalDateTime.now(), "ACTIVE");
        UUID staff = member(shelter, "STAFF");
        UUID petId = dog(shelter);
        call(put("/api/v1/pets/" + petId + "/traits"), staff, EASY_PET).andExpect(status().isOk());
        UUID fits = person();
        UUID allergic = person();
        UUID noProfile = person();
        call(put("/api/v1/users/me/adopter-profile"), fits, GOOD_HOME.formatted("")).andExpect(status().isOk());
        call(put("/api/v1/users/me/adopter-profile"), allergic, GOOD_HOME.formatted(",\"allergyDog\":true")).andExpect(status().isOk());
        String form = "{\"petId\":\"" + petId + "\",\"experience\":\"Đã nuôi chó mười năm\",\"housingCondition\":\"HOUSE\",\"liveCondition\":\"Nhà có sân\"%s}";

        call(post("/api/v1/adoptions"), allergic, form.formatted(""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MATCH_WARNING_NOT_ACKNOWLEDGED"));
        String warnedId = applicationId(call(post("/api/v1/adoptions"), allergic, form.formatted(",\"acknowledgedWarnings\":true")));
        String noProfileId = applicationId(call(post("/api/v1/adoptions"), noProfile, form.formatted("")));
        String fitsId = applicationId(call(post("/api/v1/adoptions"), fits, form.formatted("")));

        // reviewers: best match first, then the one nothing is known about, the flagged one last
        String list = call(get("/api/v1/adoptions/organization/" + shelter + "?sort=match&petId=" + petId), staff)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].match.level").value("HIGH"))
                .andExpect(jsonPath("$.data.content[0].match.score").value(100.0))
                .andExpect(jsonPath("$.data.content[1].match").doesNotExist())
                .andExpect(jsonPath("$.data.content[2].match.level").value("NOT_SUITABLE"))
                .andExpect(jsonPath("$.data.content[2].match.warnings[0].code").value("ALLERGY"))
                .andExpect(jsonPath("$.data.content[2].match.warningsAcknowledged").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(list, "$.data.content[*].applicationId")).containsExactly(fitsId, noProfileId, warnedId);
        call(get("/api/v1/adoptions/organization/" + shelter + "?petId=" + UUID.randomUUID()), staff)
                .andExpect(jsonPath("$.data.totalElements").value(0));

        // the applicant reads the level and the reasons, the reviewer also the number
        call(get("/api/v1/adoptions/" + fitsId), fits)
                .andExpect(jsonPath("$.data.match.level").value("HIGH"))
                .andExpect(jsonPath("$.data.match.score").doesNotExist())
                .andExpect(jsonPath("$.data.match.reasons.length()").value(5));
        call(get("/api/v1/adoptions/user/" + fits), fits)
                .andExpect(jsonPath("$.data.content[0].match.level").value("HIGH"))
                .andExpect(jsonPath("$.data.content[0].match.score").doesNotExist());

        // what was true at submission stays on the application
        call(put("/api/v1/pets/" + petId + "/traits"), staff, HARD_PET).andExpect(status().isOk());
        call(put("/api/v1/users/me/adopter-profile"), fits, GOOD_HOME.formatted(",\"allergyDog\":true")).andExpect(status().isOk());
        call(get("/api/v1/adoptions/" + fitsId), staff)
                .andExpect(jsonPath("$.data.match.level").value("HIGH"))
                .andExpect(jsonPath("$.data.match.score").value(100.0))
                .andExpect(jsonPath("$.data.match.weightsVersion").value(1))
                .andExpect(jsonPath("$.data.match.warnings.length()").value(0));
        assertThat(jdbc.queryForObject("SELECT match_snapshot FROM adoption_applications WHERE application_id = ?::uuid",
                String.class, fitsId)).contains("\"adopterProfile\"", "\"petTraits\"", "\"result\"");

        call(patch("/api/v1/adoptions/" + warnedId + "/reject"), staff,
                "{\"rejectReason\":\"Nhà có người dị ứng\",\"reasonCode\":\"MATCH_WARNING\"}").andExpect(status().isOk());
        call(get("/api/v1/adoptions/" + warnedId), staff)
                .andExpect(jsonPath("$.data.decisionReasonCode").value("MATCH_WARNING"));
    }

    private UUID dog(UUID shelter) {
        UUID id = pet("ORGANIZATION", shelter, shelter);
        jdbc.update("UPDATE pets SET species_id = (SELECT species_id FROM pet_species WHERE name = 'Dog') WHERE pet_id = ?", id);
        return id;
    }

    private String applicationId(ResultActions created) throws Exception {
        return JsonPath.read(created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.data.applicationId");
    }

    // ── Helpers ─────────────────────────────────────

    /** A case at (10.5, 106.7); its code is the first 12 characters of its id. */
    private UUID caseAt(UUID reporter, String species, String priority, String caseStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO rescue_cases (case_id, case_code, species, priority, status, reported_by, reported_at, created_at,
                                          location, location_text, ward_name, province_name)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ST_SetSRID(ST_MakePoint(106.7, 10.5), 4326), 'Cầu Thị Nghè', 'Phường 1', 'Hồ Chí Minh')
                """, id, id.toString().substring(0, 12), species, priority, caseStatus, reporter);
        return id;
    }

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

    private UUID reportAt(UUID reporter, String species, double lat, double lng) throws Exception {
        String speciesJson = species == null ? "null" : "\"" + species + "\"";
        String body = call(post("/api/v1/rescue-cases"), reporter, """
                {"species":%s,"priority":"HIGH","description":"Bị thương","latitude":%s,"longitude":%s,
                 "locationText":"Ngã tư","provinceName":"Đồng Nai","provinceCode":"75","contactPhone":"0900000000"}
                """.formatted(speciesJson, lat, lng)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
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
