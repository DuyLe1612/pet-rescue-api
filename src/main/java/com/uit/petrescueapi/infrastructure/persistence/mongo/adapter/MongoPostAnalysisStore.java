package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.infrastructure.persistence.mongo.MongoCollections;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostAnalysisRunDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostAnalysisRunDocument.HumanLabelDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.AnalysisDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument.FacetsDocument;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Analysis state lives on the post ({@code analysis}, {@code facets}, {@code rescueSuggestion});
 * every model call is a {@code post_analysis_runs} document. Writes made on behalf of the model
 * are guarded so they never overwrite a human decision (CONFIRMED / REJECTED).
 */
@Component
@RequiredArgsConstructor
public class MongoPostAnalysisStore implements PostAnalysisStorePort {

    private static final List<String> FINAL = List.of(PostAnalysisStatus.CONFIRMED.name(), PostAnalysisStatus.REJECTED.name());

    private final MongoTemplate mongo;

    @Override
    public Optional<AnalysisTarget> findTarget(UUID postId) {
        return Optional.ofNullable(mongo.findOne(query(where("_id").is(postId).and("deleted").is(false)), PostDocument.class))
                .map(MongoPostAnalysisStore::toTarget);
    }

    @Override
    public Optional<Claim> claim(UUID postId, int maxLifetimeAttempts, LocalDateTime staleBefore) {
        Criteria claimable = new Criteria().orOperator(
                where("analysis.status").in(PostAnalysisStatus.PENDING.name(), PostAnalysisStatus.FAILED.name()),
                where("analysis.status").exists(false),
                where("analysis.status").is(PostAnalysisStatus.ANALYZING.name()).and("analysis.updatedAt").lt(staleBefore));
        Criteria budget = new Criteria().orOperator(
                where("analysis.totalAttempts").lt(maxLifetimeAttempts), where("analysis.totalAttempts").exists(false));
        UUID token = UUID.randomUUID();
        PostDocument claimed = mongo.findAndModify(
                query(new Criteria().andOperator(where("_id").is(postId), where("deleted").is(false), claimable, budget)),
                new Update()
                        .set("analysis.status", PostAnalysisStatus.ANALYZING.name())
                        .set("analysis.claimToken", token)
                        .set("analysis.updatedAt", LocalDateTime.now())
                        .inc("analysis.attempts", 1)
                        .inc("analysis.totalAttempts", 1),
                FindAndModifyOptions.options().returnNew(true),
                PostDocument.class);
        return Optional.ofNullable(claimed).map(d -> new Claim(toTarget(d), token));
    }

    private static AnalysisTarget toTarget(PostDocument d) {
        AnalysisDocument a = d.getAnalysis();
        FacetsDocument f = d.getFacets();
        return new AnalysisTarget(
                d.getId(),
                d.getAuthor() == null ? null : d.getAuthor().getUserId(),
                d.getContent(),
                a == null || a.getStatus() == null ? PostAnalysisStatus.PENDING : PostAnalysisStatus.valueOf(a.getStatus()),
                a == null ? null : a.getSuggested(),
                f == null || f.getCategory() == null ? null : new PostFacets(PostCategory.valueOf(f.getCategory()),
                        f.getSpecies(), f.getDistrict(), f.getProvince(), Boolean.TRUE.equals(f.getNeedsRescue())),
                d.getRescueCase() == null ? null : d.getRescueCase().getCaseId(),
                a == null ? null : a.getModel(),
                a == null ? null : a.getAnalyzedAt(),
                d.getRescueSuggestion() == null ? "NONE" : d.getRescueSuggestion());
    }

    @Override
    public UUID saveRun(UUID postId, PostAnalysisPort.Result result, List<String> validationErrors) {
        PostAnalysisRunDocument run = PostAnalysisRunDocument.builder()
                .id(UUID.randomUUID())
                .postId(postId)
                .model(result.model())
                .promptVersion(result.promptVersion())
                .schemaVersion(PostDocument.SCHEMA_VERSION)
                .rawOutput(result.rawOutput())
                .parsed(result.suggestion())
                .failure(result.failure())
                .validationErrors(new ArrayList<>(validationErrors))
                .latencyMs(result.latencyMs())
                .inputTokens(result.inputTokens())
                .outputTokens(result.outputTokens())
                .createdAt(LocalDateTime.now())
                .build();
        mongo.insert(run);
        return run.getId();
    }

    @Override
    public void markAnalyzed(UUID postId, UUID token, UUID runId, String model, PostAnalysisSuggestion suggestion) {
        LocalDateTime now = LocalDateTime.now();
        mongo.updateFirst(claimedBy(postId, token), new Update()
                        .set("analysis.status", PostAnalysisStatus.ANALYZED.name())
                        .set("analysis.schemaVersion", PostDocument.SCHEMA_VERSION)
                        .set("analysis.model", model)
                        .set("analysis.analyzedAt", now)
                        .set("analysis.lastRunId", runId)
                        .set("analysis.lastError", null)
                        .set("analysis.claimToken", null)
                        .set("analysis.suggested", suggestion)
                        .set("analysis.updatedAt", now)
                        .set("rescueSuggestion", suggestion.needsRescue() ? "SUGGESTED" : "NONE"),
                PostDocument.class);
    }

    @Override
    public void markFailed(UUID postId, UUID token, UUID runId, String reason) {
        // the attempt was already counted by claim()
        mongo.updateFirst(claimedBy(postId, token), new Update()
                        .set("analysis.status", PostAnalysisStatus.FAILED.name())
                        .set("analysis.lastRunId", runId)
                        .set("analysis.lastError", reason)
                        .set("analysis.claimToken", null)
                        .set("analysis.updatedAt", LocalDateTime.now()),
                PostDocument.class);
    }

    @Override
    public void confirm(UUID postId, PostFacets facets, List<String> tags, UUID userId) {
        PostDocument before = mongo.findById(postId, PostDocument.class);
        LocalDateTime now = LocalDateTime.now();
        mongo.updateFirst(query(where("_id").is(postId)), new Update()
                        .set("facets", FacetsDocument.builder()
                                .category(facets.category().name())
                                .species(facets.species())
                                .district(facets.district())
                                .province(facets.province())
                                .needsRescue(facets.needsRescue())
                                .build())
                        .set("tags", tags)
                        .set("analysis.status", PostAnalysisStatus.CONFIRMED.name())
                        .set("analysis.confirmedBy", userId)
                        .set("analysis.confirmedAt", now)
                        .set("analysis.updatedAt", now)
                        .set("rescueSuggestion", rescueSuggestionAfterConfirm(before, facets)),
                PostDocument.class);

        // only an ANALYZED suggestion describes the text being confirmed (after an edit it is stale)
        AnalysisDocument analysisBefore = before == null ? null : before.getAnalysis();
        UUID runId = analysisBefore != null && PostAnalysisStatus.ANALYZED.name().equals(analysisBefore.getStatus())
                ? analysisBefore.getLastRunId() : null;
        if (runId != null) {
            mongo.updateFirst(query(where("_id").is(runId)), new Update().set("humanLabel", HumanLabelDocument.builder()
                            .category(facets.category().name())
                            .species(facets.species())
                            .district(facets.district())
                            .province(facets.province())
                            .needsRescue(facets.needsRescue())
                            .build()),
                    PostAnalysisRunDocument.class);
        }
    }

    private static String rescueSuggestionAfterConfirm(PostDocument before, PostFacets facets) {
        if (before != null && before.getRescueCase() != null) {
            return "ACCEPTED";
        }
        return facets.needsRescue() ? "SUGGESTED" : "NONE";
    }

    @Override
    public void reject(UUID postId, UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        mongo.updateFirst(query(where("_id").is(postId)), new Update()
                        .set("analysis.status", PostAnalysisStatus.REJECTED.name())
                        .set("analysis.rejectedBy", userId)
                        .set("analysis.updatedAt", now)
                        .set("rescueSuggestion", "DISMISSED"),
                PostDocument.class);
    }

    @Override
    public void resetToPending(UUID postId) {
        mongo.updateFirst(notFinal(postId), new Update()
                        .set("analysis.status", PostAnalysisStatus.PENDING.name())
                        .set("analysis.attempts", 0)
                        .set("analysis.claimToken", null)
                        .set("analysis.lastError", null)
                        .set("analysis.updatedAt", LocalDateTime.now()),
                PostDocument.class);
    }

    /** {@code rescueCase: null} also matches a missing field, so this is a single atomic check-and-set. */
    @Override
    public boolean reserveRescueCase(UUID postId) {
        return mongo.updateFirst(query(where("_id").is(postId).and("rescueCase").is(null)),
                new Update().set("rescueCase", new PostDocument.RescueCaseRefDocument(null, "CREATING")),
                PostDocument.class).getModifiedCount() == 1;
    }

    @Override
    public void releaseRescueCase(UUID postId) {
        mongo.updateFirst(query(where("_id").is(postId).and("rescueCase.caseId").is(null)),
                new Update().unset("rescueCase"), PostDocument.class);
    }

    @Override
    public void linkRescueCase(UUID postId, UUID caseId, String status) {
        mongo.updateFirst(query(where("_id").is(postId)), new Update()
                        .set("rescueCase", new PostDocument.RescueCaseRefDocument(caseId, status))
                        .set("rescueSuggestion", "ACCEPTED"),
                PostDocument.class);
    }

    @Override
    public List<UUID> findRetryable(int maxAttempts, int maxLifetimeAttempts, LocalDateTime before, LocalDateTime staleBefore, int limit) {
        Criteria queued = new Criteria().andOperator(
                where("analysis.status").in(PostAnalysisStatus.PENDING.name(), PostAnalysisStatus.FAILED.name()),
                new Criteria().orOperator(where("analysis.attempts").lt(maxAttempts), where("analysis.attempts").exists(false)),
                new Criteria().orOperator(where("analysis.updatedAt").lt(before), where("analysis.updatedAt").exists(false)));
        Criteria stale = where("analysis.status").is(PostAnalysisStatus.ANALYZING.name()).and("analysis.updatedAt").lt(staleBefore);
        Criteria legacy = where("analysis").exists(false); // posts created before the analysis feature
        Criteria budget = new Criteria().orOperator(
                where("analysis.totalAttempts").lt(maxLifetimeAttempts), where("analysis.totalAttempts").exists(false));
        Query q = query(new Criteria().andOperator(where("deleted").is(false), budget,
                        new Criteria().orOperator(queued, stale, legacy)))
                .with(Sort.by(Sort.Order.asc("createdAt")))
                .limit(limit);
        q.fields().include("_id");
        return mongo.find(q, PostDocument.class).stream().map(PostDocument::getId).toList();
    }

    /** Accuracy is counted only over runs that produced output and got a human label. */
    @Override
    public List<PostAnalysisMetricsDto> metrics() {
        Document labelled = and(exists("$humanLabel"), exists("$parsed"));
        Document group = new Document("_id", "$promptVersion")
                .append("runs", new Document("$sum", 1))
                .append("failed", count(exists("$failure")))
                .append("invalid", count(new Document("$gt", List.of(new Document("$size",
                        new Document("$ifNull", List.of("$validationErrors", List.of()))), 0))))
                .append("labelled", count(labelled))
                .append("categoryOk", count(and(labelled, eq("$parsed.category", "$humanLabel.category"))))
                .append("speciesOk", count(and(labelled, eq("$parsed.species", "$humanLabel.species"))))
                .append("districtOk", count(and(labelled, eq("$parsed.district", "$humanLabel.district"))))
                .append("rescueOk", count(and(labelled, eq("$parsed.needsRescue", "$humanLabel.needsRescue"))));
        List<Document> pipeline = List.of(new Document("$group", group), new Document("$sort", new Document("_id", 1)));

        List<PostAnalysisMetricsDto> result = new ArrayList<>();
        for (Document d : mongo.getCollection(MongoCollections.POST_ANALYSIS_RUNS).aggregate(pipeline)) {
            long n = number(d, "labelled");
            result.add(new PostAnalysisMetricsDto(
                    d.getInteger("_id", 0),
                    number(d, "runs"),
                    number(d, "failed"),
                    number(d, "invalid"),
                    n,
                    ratio(number(d, "categoryOk"), n),
                    ratio(number(d, "speciesOk"), n),
                    ratio(number(d, "districtOk"), n),
                    ratio(number(d, "rescueOk"), n)));
        }
        return result;
    }

    private Query claimedBy(UUID postId, UUID token) {
        return query(where("_id").is(postId)
                .and("analysis.status").is(PostAnalysisStatus.ANALYZING.name())
                .and("analysis.claimToken").is(token));
    }

    private Query notFinal(UUID postId) {
        return query(where("_id").is(postId).and("analysis.status").nin(FINAL));
    }

    // ── aggregation expression helpers ──
    /** {@code $gt: [x, null]} is false for both missing and null fields. */
    private static Document exists(String field) {
        return new Document("$gt", java.util.Arrays.asList(field, null));
    }

    private static Document and(Document a, Document b) {
        return new Document("$and", List.of(a, b));
    }

    private static Document eq(String a, String b) {
        return new Document("$eq", List.of(a, b));
    }

    private static Document count(Document condition) {
        return new Document("$sum", new Document("$cond", List.of(condition, 1, 0)));
    }

    private static long number(Document d, String key) {
        Number value = d.get(key, Number.class);
        return value == null ? 0 : value.longValue();
    }

    private static Double ratio(long ok, long total) {
        return total == 0 ? null : (double) ok / total;
    }
}
