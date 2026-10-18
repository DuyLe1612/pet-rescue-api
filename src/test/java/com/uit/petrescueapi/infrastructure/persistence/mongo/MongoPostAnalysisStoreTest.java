package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort.AnalysisTarget;
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

    private UUID run(UUID postId, PostAnalysisSuggestion s, String failure, List<String> errors) {
        return store.saveRun(postId, new PostAnalysisPort.Result("claude-opus-5-5", 1, "{}", s, failure, 100, 10, 5), errors);
    }

    @Test
    void analyzedSuggestionIsStoredAndRescueIsSuggested() {
        UUID id = pendingPost();
        UUID runId = run(id, rescueDog, null, List.of());

        store.markAnalyzed(id, runId, "claude-opus-5-5", rescueDog);

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
        UUID runId = run(id, rescueDog, null, List.of());
        store.markAnalyzed(id, runId, "claude-opus-5-5", rescueDog);

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
    void lateModelResultDoesNotOverwriteAHumanDecision() {
        UUID id = pendingPost();
        store.confirm(id, new PostFacets(PostCategory.GENERAL, null, null, null, false), List.of(), AUTHOR);

        store.markAnalyzed(id, run(id, rescueDog, null, List.of()), "claude-opus-5-5", rescueDog);
        store.markFailed(id, run(id, null, "timeout", List.of()), "timeout");
        store.resetToPending(id);

        assertThat(store.findTarget(id).orElseThrow().status()).isEqualTo(PostAnalysisStatus.CONFIRMED);
    }

    @Test
    void retryQueueHoldsPendingAndFailedUnderTheAttemptLimit() {
        UUID pending = pendingPost();
        UUID failedOnce = pendingPost();
        store.markFailed(failedOnce, run(failedOnce, null, "overloaded", List.of()), "overloaded");
        UUID exhausted = pendingPost();
        for (int i = 0; i < 3; i++) store.markFailed(exhausted, run(exhausted, null, "x", List.of()), "x");
        UUID done = pendingPost();
        store.markAnalyzed(done, run(done, rescueDog, null, List.of()), "m", rescueDog);
        UUID legacy = post(null);                                       // created before analysis existed

        List<UUID> queue = store.findRetryable(3, LocalDateTime.now().plusSeconds(1), 10);

        assertThat(queue).containsExactlyInAnyOrder(pending, failedOnce, legacy);
    }

    @Test
    void rescueCaseLinkMarksSuggestionAccepted() {
        UUID id = pendingPost();
        UUID caseId = UUID.randomUUID();
        store.linkRescueCase(id, caseId, "REPORTED");

        AnalysisTarget t = store.findTarget(id).orElseThrow();
        assertThat(t.rescueCaseId()).isEqualTo(caseId);
        assertThat(t.rescueSuggestion()).isEqualTo("ACCEPTED");
    }

    @Test
    void metricsCompareModelOutputWithHumanLabelsPerPromptVersion() {
        // post 1: model right on everything
        UUID p1 = pendingPost();
        store.markAnalyzed(p1, run(p1, rescueDog, null, List.of()), "m", rescueDog);
        store.confirm(p1, new PostFacets(PostCategory.RESCUE, "Dog", "Quận 7", null, true), List.of(), AUTHOR);
        // post 2: model got species + rescue wrong, output had a validation error
        UUID p2 = pendingPost();
        store.markAnalyzed(p2, run(p2, rescueDog, null, List.of("unknown species: Dragon")), "m", rescueDog);
        store.confirm(p2, new PostFacets(PostCategory.RESCUE, "Cat", "Quận 7", null, false), List.of(), AUTHOR);
        // post 3: failed call, never labelled
        UUID p3 = pendingPost();
        store.markFailed(p3, run(p3, null, "refusal", List.of()), "refusal");

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
