package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.command.RescueTriageCommandPort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.application.service.TriageRules;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseTriage;
import com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI triage of a reported rescue case. The model only <b>labels</b>: it never edits the report,
 * never lowers a priority (see {@link RescueCaseTriage#effectivePriority}), and never closes or
 * merges a case. Nothing in reporting waits for it, and a case with no triage or a failed one is
 * handled exactly as before.
 *
 * <p>It reuses the post analysis model, prompt and output schema: that output already carries the
 * urgency, a condition summary and a normalised species and area, so the three providers need no
 * second code path. Registered by {@code RescueTriageConfig}; no transaction, each store call is
 * one statement.</p>
 */
@Slf4j
public class RescueTriageUseCase implements RescueTriageCommandPort {

    /** Paid model calls per case over its whole life. */
    public static final int MAX_ATTEMPTS = 3;
    /** A claim older than this belongs to a worker that died mid-call. */
    static final Duration CLAIM_TIMEOUT = Duration.ofMinutes(5);

    private final RescueCaseTriageRepository triage;
    private final Optional<PostAnalysisPort> ai;
    private final AnalysisCatalogPort catalog;
    private final PostAnalysisValidator validator;
    private final PostAnalysisCommandPort models;
    private final RescueCaseDomainService cases;

    public RescueTriageUseCase(RescueCaseTriageRepository triage, Optional<PostAnalysisPort> ai, AnalysisCatalogPort catalog,
                               PostAnalysisValidator validator, PostAnalysisCommandPort models, RescueCaseDomainService cases) {
        this.triage = triage;
        this.ai = ai;
        this.catalog = catalog;
        this.validator = validator;
        this.models = models;
        this.cases = cases;
    }

    @Override
    public void triage(UUID caseId) {
        if (ai.isEmpty()) {
            log.debug("AI disabled; rescue case {} is not triaged", caseId);
            return;
        }
        RescueCase rescueCase = cases.findById(caseId);
        if (rescueCase.getStatus() != RescueCaseStatus.REPORTED && rescueCase.getStatus() != RescueCaseStatus.IN_PROGRESS) {
            return; // nobody needs a label on a finished case
        }
        if (!triage.claim(caseId, MAX_ATTEMPTS, LocalDateTime.now().minus(CLAIM_TIMEOUT))) {
            return; // labelled, held by another worker right now, or out of attempts
        }

        // Rules first: they hold whether or not the model answers.
        List<String> missing = TriageRules.missingFields(rescueCase, triage.countPhotos(caseId));
        Catalog values = new Catalog(catalog.speciesNames(), List.of());
        PostAnalysisPort.Result result = ai.get().analyze(TriageRules.textFor(rescueCase), values, models.modelSettings().activeModel());

        if (!result.succeeded()) {
            triage.markFailed(caseId, result.failure(), result.rawOutput(), result.latencyMs());
            return;
        }
        PostAnalysisValidator.Outcome outcome = validator.validate(result.suggestion(), values);
        if (outcome.fatal()) {
            triage.markFailed(caseId, "invalid model output: " + String.join("; ", outcome.errors()),
                    result.rawOutput(), result.latencyMs());
            return;
        }
        PostAnalysisSuggestion s = outcome.suggestion();
        triage.markAnalyzed(RescueCaseTriage.builder()
                .caseId(caseId)
                .model(result.model())
                .promptVersion(result.promptVersion())
                .aiUrgency(s.needsRescue() ? s.urgency() : null)
                .needsRescue(s.needsRescue())
                .conditionSummary(s.rescueSummary())
                .species(s.species())
                .district(s.district())
                .province(s.province())
                .missingFields(missing)
                .rawOutput(result.rawOutput())
                .latencyMs(result.latencyMs())
                .inputTokens(result.inputTokens())
                .outputTokens(result.outputTokens())
                .build());
    }
}
