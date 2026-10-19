package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort.AnalysisTarget;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort.Claim;
import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoPostAnalysisStore;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.AuthorDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import com.uit.petrescueapi.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MongoPostAnalysisStoreTest extends MongoIntegrationTest {

    static final UUID AUTHOR = UUID.randomUUID();
    static final int LIFETIME = 4;

    @Autowired MongoTemplate mongo;
    MongoPostAnalysisStore store;

    final PostAnalysisSuggestion rescueDog = new PostAnalysisSuggestion(PostCategory.RESCUE, "Dog", "Q7", "Quận 7", null,
            true, "Possible leg injury", RescuePriority.HIGH, List.of("injured"), 0.95, 0.9, 0.8);

    @BeforeEach
    void setUp() {
        mongo.getDb().drop();
        new MongoSchemaInitializer(mongo).run(null);
        store = new MongoPostAnalysisStore(mongo);
    }

    private UUID post(PostDocument.AnalysisDocument analysis) {
        UUID id = UUID.randomUUID();
        mongo.insert(PostDocument.builder().id(id).author(AuthorDocument.builder().userId(AUTHOR).build())
                .content("bé cún bị thương").analysis(analysis).createdAt(LocalDateTime.now()).build());
        return id;
    }

    private UUID pendingPost() {
        return post(PostDocument.AnalysisDocument.pending(LocalDateTime.now()));
    }

    private Claim claim(UUID id) {
        return store.claim(id, LIFETIME, LocalDateTime.now().minusMinutes(5)).orElseThrow();
    }

    private UUID run(UUID postId, PostAnalysisSuggestion s, String failure, List<String> errors) {
        return store.saveRun(postId, new PostAnalysisPort.Result("claude-opus-5-5", 1, "{}", s, failure, 100, 10, 5), errors);
    }

    private void analyzed(UUID id, List<String> errors) {
        Claim c = claim(id);
        store.markAnalyzed(id, c.token(), run(id, rescueDog, null, errors), "claude-opus-5-5", rescueDog);
    }

    private void failed(UUID id) {
        Claim c = claim(id);
        store.markFailed(id, c.token(), run(id, null, "overloaded", List.of()), "overloaded");
    }

    private Document analysisDoc(UUID id) {
        return mongo.getCollection(MongoCollections.POSTS).find(new Document("_id", id)).first().get("analysis", Document.class);
    }

    // ── claim ───────────────────────────────────────────────

    @Test
    void claimIsExclusiveUntilTheResultIsWritten() {
        UUID id = pendingPost();

        Claim first = store.claim(id, LIFETIME, LocalDateTime.now().minusMinutes(5)).orElseThrow();

        assertThat(first.target().content()).isEqualTo("bé cún bị thương");
        assertThat(analysisDoc(id)).containsEntry("status", "ANALYZING").containsEntry("totalAttempts", 1);
        assertThat(store.claim(id, LIFETIME, LocalDateTime.now().minusMinutes(5))).isEmpty(); // event + poller can't both bill
    }

    @Test
    void lifetimeCapSurvivesRetriesAndEdits() {
        UUID id = pendingPost();
        for (int i = 0; i < LIFETIME; i++) {
            failed(id);
            store.resetToPending(id);                                   // what retry and a content edit do
        }
        assertThat(store.claim(id, LIFETIME, LocalDateTime.now().minusMinutes(5))).isEmpty();
        assertThat(store.findRetryable(10, LIFETIME, LocalDateTime.now().plusSeconds(1), LocalDateTime.now().minusMinutes(5), 10)).doesNotContain(id);
    }

    @Test
    void staleClaimFromACrashedWorkerCanBeTakenOver() {
        UUID id = pendingPost();
        claim(id);
        mongo.getCollection(MongoCollections.POSTS).updateOne(new Document("_id", id),
                new Document("$set", new Document("analysis.updatedAt", java.util.Date.from(java.time.Instant.now().minusSeconds(3600)))));

        assertThat(store.findRetryable(3, LIFETIME, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5), 10)).contains(id);
        assertThat(store.claim(id, LIFETIME, LocalDateTime.now().minusMinutes(5))).isPresent();
    }

    @Test
    void resultForAnOutdatedClaimIsDropped() {
        UUID id = pendingPost();
        Claim stale = claim(id);
        store.resetToPending(id);                                       // author edited the text meanwhile

        store.markAnalyzed(id, stale.token(), run(id, rescueDog, null, List.of()), "m", rescueDog);

        assertThat(store.findTarget(id).orElseThrow().status()).isEqualTo(PostAnalysisStatus.PENDING);
        assertThat(store.findTarget(id).orElseThrow().suggestion()).isNull();
    }

    // ── results / decisions ─────────────────────────────────

    @Test
    void analyzedSuggestionIsStoredAndRescueIsSuggested() {
        UUID id = pendingPost();
        analyzed(id, List.of());

        AnalysisTarget t = store.findTarget(id).orElseThrow();
        assertThat(t.status()).isEqualTo(PostAnalysisStatus.ANALYZED);
        assertThat(t.suggestion()).isEqualTo(rescueDog);
        assertThat(t.authorId()).isEqualTo(AUTHOR);
        assertThat(t.rescueSuggestion()).isEqualTo("SUGGESTED");
        assertThat(t.facets()).isNull();                                // AI never writes facets
    }

    @Test
    void confirmWritesFacetsTagsAndLabelsTheRun() {
        UUID id = pendingPost();
        Claim c = claim(id);
        UUID runId = run(id, rescueDog, null, List.of());
        store.markAnalyzed(id, c.token(), runId, "claude-opus-5-5", rescueDog);

        PostFacets facets = new PostFacets(PostCategory.RESCUE, "Cat", "Quận 7", "TP. Hồ Chí Minh", true);
        store.confirm(id, facets, List.of("injured", "cat"), AUTHOR);

        AnalysisTarget t = store.findTarget(id).orElseThrow();
        assertThat(t.status()).isEqualTo(PostAnalysisStatus.CONFIRMED);
        assertThat(t.facets()).isEqualTo(facets);
        Document postDoc = mongo.getCollection(MongoCollections.POSTS).find(new Document("_id", id)).first();
        assertThat(postDoc.getList("tags", String.class)).containsExactly("injured", "cat");
        Document runDoc = mongo.getCollection(MongoCollections.POST_ANALYSIS_RUNS).find(new Document("_id", runId)).first();
        assertThat(runDoc.get("humanLabel", Document.class)).containsEntry("species", "Cat").containsEntry("needsRescue", true);
    }

    @Test
    void confirmAfterAnEditDoesNotLabelTheRunOfTheOldText() {
        UUID id = pendingPost();
        Claim c = claim(id);
        UUID runId = run(id, rescueDog, null, List.of());
        store.markAnalyzed(id, c.token(), runId, "m", rescueDog);
        store.resetToPending(id);                                       // edited: suggestion described the old text

        store.confirm(id, new PostFacets(PostCategory.GENERAL, null, null, null, false), List.of(), AUTHOR);

        Document runDoc = mongo.getCollection(MongoCollections.POST_ANALYSIS_RUNS).find(new Document("_id", runId)).first();
        assertThat(runDoc.get("humanLabel")).isNull();
    }

    @Test
    void lateModelResultDoesNotOverwriteAHumanDecision() {
        UUID id = pendingPost();
        Claim c = claim(id);
        store.confirm(id, new PostFacets(PostCategory.GENERAL, null, null, null, false), List.of(), AUTHOR);

        store.markAnalyzed(id, c.token(), run(id, rescueDog, null, List.of()), "claude-opus-5-5", rescueDog);
        store.markFailed(id, c.token(), run(id, null, "timeout", List.of()), "timeout");
        store.resetToPending(id);

        assertThat(store.findTarget(id).orElseThrow().status()).isEqualTo(PostAnalysisStatus.CONFIRMED);
        assertThat(store.claim(id, LIFETIME, LocalDateTime.now().minusMinutes(5))).isEmpty();
    }

    @Test
    void retryQueueHoldsPendingAndFailedUnderTheAttemptLimit() {
        UUID pending = pendingPost();
        UUID failedOnce = pendingPost();
        failed(failedOnce);
        UUID exhausted = pendingPost();
        for (int i = 0; i < 3; i++) failed(exhausted);
        UUID done = pendingPost();
        analyzed(done, List.of());
        UUID legacy = post(null);                                       // created before analysis existed

        List<UUID> queue = store.findRetryable(3, LIFETIME, LocalDateTime.now().plusSeconds(1), LocalDateTime.now().minusMinutes(5), 10);

        assertThat(queue).containsExactlyInAnyOrder(pending, failedOnce, legacy);
    }

    // ── rescue case link ────────────────────────────────────

    @Test
    void rescueCaseReservationIsExclusive() {
        UUID id = pendingPost();

        assertThat(store.reserveRescueCase(id)).isTrue();
        assertThat(store.reserveRescueCase(id)).isFalse();             // double tap

        store.releaseRescueCase(id);
        assertThat(store.reserveRescueCase(id)).isTrue();
    }

    @Test
    void rescueCaseLinkMarksSuggestionAccepted() {
        UUID id = pendingPost();
        UUID caseId = UUID.randomUUID();
        store.reserveRescueCase(id);
        store.linkRescueCase(id, caseId, "REPORTED");

        AnalysisTarget t = store.findTarget(id).orElseThrow();
        assertThat(t.rescueCaseId()).isEqualTo(caseId);
        assertThat(t.rescueSuggestion()).isEqualTo("ACCEPTED");
        assertThat(store.reserveRescueCase(id)).isFalse();
    }

    // ── metrics ─────────────────────────────────────────────

    @Test
    void metricsCompareModelOutputWithHumanLabelsPerPromptVersion() {
        UUID p1 = pendingPost();
        analyzed(p1, List.of());
        store.confirm(p1, new PostFacets(PostCategory.RESCUE, "Dog", "Quận 7", null, true), List.of(), AUTHOR);
        UUID p2 = pendingPost();
        analyzed(p2, List.of("unknown species: Dragon"));
        store.confirm(p2, new PostFacets(PostCategory.RESCUE, "Cat", "Quận 7", null, false), List.of(), AUTHOR);
        UUID p3 = pendingPost();
        failed(p3);

        PostAnalysisMetricsDto m = store.metrics().get(0);

        assertThat(m.promptVersion()).isEqualTo(1);
        assertThat(m.runs()).isEqualTo(3);
        assertThat(m.failed()).isEqualTo(1);
        assertThat(m.withValidationErrors()).isEqualTo(1);
        assertThat(m.labelled()).isEqualTo(2);
        assertThat(m.categoryAccuracy()).isEqualTo(1.0);
        assertThat(m.speciesAccuracy()).isEqualTo(0.5);
        assertThat(m.districtAccuracy()).isEqualTo(1.0);
        assertThat(m.needsRescueAccuracy()).isEqualTo(0.5);
    }
}
