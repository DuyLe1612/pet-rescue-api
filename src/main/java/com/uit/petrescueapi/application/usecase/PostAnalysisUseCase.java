package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.analysis.ConfirmPostAnalysisRequestDto;
import com.uit.petrescueapi.application.dto.analysis.CreateRescueCaseFromPostRequestDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisResponseDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort.AnalysisTarget;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DA2 Goal 1 + 3. The model only <b>suggests</b>; the author confirms or corrects, and only the
 * confirmed facets feed search and the Rescue Case workflow. Rescue Case rules stay in
 * {@link RescueCaseCommandPort} (PostgreSQL).
 *
 * <p>No {@code @Transactional}: every store call is a single-document MongoDB write. Registered by
 * {@code PostAnalysisConfig}.</p>
 */
@Slf4j
public class PostAnalysisUseCase implements PostAnalysisCommandPort {

    /** Paid model calls per post over its whole life, whatever retries or edits happen. */
    public static final int MAX_LIFETIME_ATTEMPTS = 6;
    /** A claim older than this belongs to a worker that died mid-call. */
    static final java.time.Duration CLAIM_TIMEOUT = java.time.Duration.ofMinutes(5);

    private final PostAnalysisStorePort store;
    private final Optional<PostAnalysisPort> ai;
    private final AnalysisCatalogPort catalog;
    private final PostAnalysisValidator validator;
    private final RescueCaseCommandPort rescueCases;

    public PostAnalysisUseCase(PostAnalysisStorePort store, Optional<PostAnalysisPort> ai, AnalysisCatalogPort catalog,
                               PostAnalysisValidator validator, RescueCaseCommandPort rescueCases) {
        this.store = store;
        this.ai = ai;
        this.catalog = catalog;
        this.validator = validator;
        this.rescueCases = rescueCases;
    }

    @Override
    public void analyze(UUID postId) {
        if (ai.isEmpty()) {
            log.debug("AI post analysis disabled; post {} stays PENDING", postId);
            return;
        }
        PostAnalysisStorePort.Claim claim = store.claim(postId, MAX_LIFETIME_ATTEMPTS,
                java.time.LocalDateTime.now().minus(CLAIM_TIMEOUT)).orElse(null);
        if (claim == null) {
            return; // decided by a human, analysed by another worker right now, or out of budget
        }
        UUID token = claim.token();
        Catalog values = catalog();
        PostAnalysisPort.Result result = ai.get().analyze(claim.target().content(), values);

        if (!result.succeeded()) {
            UUID runId = store.saveRun(postId, result, List.of());
            store.markFailed(postId, token, runId, result.failure());
            return;
        }
        PostAnalysisValidator.Outcome outcome = validator.validate(result.suggestion(), values);
        PostAnalysisPort.Result checked = new PostAnalysisPort.Result(result.model(), result.promptVersion(), result.rawOutput(),
                outcome.suggestion(), null, result.latencyMs(), result.inputTokens(), result.outputTokens());
        UUID runId = store.saveRun(postId, checked, outcome.errors());
        if (outcome.fatal()) {
            store.markFailed(postId, token, runId, "invalid model output: " + String.join("; ", outcome.errors()));
        } else {
            store.markAnalyzed(postId, token, runId, result.model(), outcome.suggestion());
        }
    }

    @Override
    public PostAnalysisResponseDto getAnalysis(UUID postId) {
        return toDto(load(postId));
    }

    @Override
    public PostAnalysisResponseDto confirm(UUID postId, ConfirmPostAnalysisRequestDto request, UUID userId) {
        AnalysisTarget target = loadOwned(postId, userId);
        Catalog values = catalog();

        String species = null;
        if (request.species() != null && !request.species().isBlank()) {
            species = values.speciesNames().stream().filter(n -> n.equalsIgnoreCase(request.species().trim())).findFirst()
                    .orElseThrow(() -> new BusinessException("Unknown species: " + request.species(), "POST_ANALYSIS_SPECIES"));
        }
        List<String> tags = new ArrayList<>(new LinkedHashSet<>(Optional.ofNullable(request.tags()).orElse(List.of()).stream()
                .filter(Objects::nonNull).map(t -> t.trim().toLowerCase(Locale.ROOT)).toList()));
        List<String> unknown = tags.stream().filter(t -> !values.tagCodes().contains(t)).toList();
        if (!unknown.isEmpty()) {
            throw new BusinessException("Unknown tags: " + unknown, "POST_ANALYSIS_TAGS");
        }

        PostFacets facets = new PostFacets(request.category(), species, blankToNull(request.district()),
                blankToNull(request.province()), request.needsRescue());
        store.confirm(postId, facets, tags, userId);
        return toDto(load(target.postId()));
    }

    @Override
    public PostAnalysisResponseDto reject(UUID postId, UUID userId) {
        loadOwned(postId, userId);
        store.reject(postId, userId);
        return toDto(load(postId));
    }

    @Override
    public PostAnalysisResponseDto retry(UUID postId, UUID userId) {
        AnalysisTarget target = loadOwned(postId, userId);
        if (target.status() != PostAnalysisStatus.PENDING && target.status() != PostAnalysisStatus.FAILED) {
            throw new BusinessException("Only a pending or failed analysis can be retried (is " + target.status() + ")",
                    "POST_ANALYSIS_NOT_RETRYABLE");
        }
        store.resetToPending(postId);
        return toDto(load(postId));
    }

    @Override
    public RescueCase createRescueCase(UUID postId, CreateRescueCaseFromPostRequestDto request, UUID userId) {
        AnalysisTarget target = loadOwned(postId, userId);
        if (target.rescueCaseId() != null) {
            throw new BusinessException("Post already has a rescue case", "POST_RESCUE_CASE_EXISTS");
        }
        if (target.status() != PostAnalysisStatus.CONFIRMED || target.facets() == null || !target.facets().needsRescue()) {
            throw new BusinessException("Confirm the analysis as a rescue first", "POST_NOT_CONFIRMED_RESCUE");
        }
        PostFacets facets = target.facets();
        PostAnalysisSuggestion suggestion = target.suggestion();
        RescuePriority priority = suggestion != null && suggestion.urgency() != null ? suggestion.urgency() : RescuePriority.NORMAL;

        CreateRescueCaseRequestDto command = CreateRescueCaseRequestDto.builder()
                .species(facets.species())
                .description(target.content())
                .locationText(Stream.of(facets.district(), facets.province()).filter(Objects::nonNull).collect(Collectors.joining(", ")))
                .provinceName(facets.province())
                .priority(priority)
                .latitude(request == null ? null : request.latitude())
                .longitude(request == null ? null : request.longitude())
                .contactPhone(request == null ? null : request.contactPhone())
                .build();
        // reserve first: a double tap must not create two public rescue cases
        if (!store.reserveRescueCase(postId)) {
            throw new BusinessException("A rescue case is already being created for this post", "POST_RESCUE_CASE_EXISTS");
        }
        RescueCase created;
        try {
            created = rescueCases.report(command, userId);
        } catch (RuntimeException e) {
            store.releaseRescueCase(postId);
            throw e;
        }
        store.linkRescueCase(postId, created.getCaseId(), created.getStatus() == null ? null : created.getStatus().name());
        return created;
    }

    @Override
    public List<PostAnalysisMetricsDto> metrics() {
        return store.metrics();
    }

    private Catalog catalog() {
        return new Catalog(catalog.speciesNames(), catalog.tagCodes());
    }

    private AnalysisTarget load(UUID postId) {
        return store.findTarget(postId).orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));
    }

    private AnalysisTarget loadOwned(UUID postId, UUID userId) {
        AnalysisTarget target = load(postId);
        if (!target.authorId().equals(userId)) {
            throw new BusinessException("Only the author can do this", "POST_FORBIDDEN");
        }
        return target;
    }

    private static PostAnalysisResponseDto toDto(AnalysisTarget t) {
        return PostAnalysisResponseDto.builder()
                .postId(t.postId())
                .status(t.status())
                .model(t.model())
                .analyzedAt(t.analyzedAt())
                .suggestion(t.suggestion())
                .facets(t.facets())
                .rescueSuggestion(t.rescueSuggestion())
                .rescueCaseId(t.rescueCaseId())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
