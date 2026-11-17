package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.adoption.CreateAdoptionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.DecisionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.ReclaimPetRequestDto;
import com.uit.petrescueapi.application.port.command.AdoptionCommandPort;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.query.MatchingQueryPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.ReclaimLog;
import com.uit.petrescueapi.domain.service.AdoptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command (write) use-case for Adoption operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link AdoptionDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionCommandUseCase implements AdoptionCommandPort {

    private final AdoptionDomainService domainService;
    private final MediaQueryPort mediaQueryPort;
    private final MediaCommandPort mediaCommandPort;
    private final MatchingQueryPort matchingQueryPort;
    private final ObjectMapper objectMapper;

    @Override
    public AdoptionApplication submit(CreateAdoptionRequestDto cmd, UUID applicantId) {
        log.debug("Command: submit adoption application for pet {} by user {}", cmd.getPetId(), applicantId);
        // The signature is optional: the organization decides, the form does not block (same spirit as Q2).
        if (cmd.getSignatureMediaId() != null) {
            mediaQueryPort.findById(cmd.getSignatureMediaId());
            mediaCommandPort.confirmUpload(cmd.getSignatureMediaId(), "adoptions/signatures/" + applicantId);
        }
        AdoptionApplication application = AdoptionApplication.builder()
                .petId(cmd.getPetId())
                .organizationId(cmd.getOrganizationId())
                .applicantId(applicantId)
                .experience(cmd.getExperience())
                .housingCondition(cmd.getHousingCondition())
                .liveCondition(cmd.getLiveCondition())
                .signatureMediaId(cmd.getSignatureMediaId())
                .warningsAcknowledged(cmd.isAcknowledgedWarnings())
                .build();
        // No adopter profile means nothing to match against: the application goes through without a result.
        matchingQueryPort.evaluate(applicantId, cmd.getPetId()).ifPresent(evaluation -> freezeMatch(application, evaluation));
        // The domain service stores the in-app notification; delivery (WebSocket + Expo) happens after commit.
        return domainService.submit(application);
    }

    @Override
    public AdoptionApplication approve(UUID applicationId, DecisionRequestDto decision, UUID decidedBy) {
        log.debug("Command: approve adoption application {}", applicationId);
        return domainService.approve(applicationId, decidedBy, decision.getReadyAt(), decision.getReasonCode());
    }

    @Override
    public AdoptionApplication reject(UUID applicationId, DecisionRequestDto decision, UUID decidedBy) {
        log.debug("Command: reject adoption application {}", applicationId);
        return domainService.reject(applicationId, decidedBy, decision.getRejectReason(), decision.getReasonCode());
    }

    @Override
    public AdoptionApplication cancel(UUID applicationId, UUID requesterId) {
        log.debug("Command: cancel adoption application {}", applicationId);
        return domainService.cancel(applicationId, requesterId);
    }

    @Override
    public AdoptionApplication complete(UUID applicationId, UUID completedBy) {
        log.debug("Command: complete adoption application {}", applicationId);
        return domainService.complete(applicationId, completedBy);
    }

    @Override
    public ReclaimLog reclaim(UUID applicationId, ReclaimPetRequestDto request, UUID reclaimedBy) {
        log.debug("Command: reclaim adoption application {}", applicationId);
        mediaQueryPort.findById(request.getProofId());
        mediaCommandPort.confirmUpload(request.getProofId(), "adoptions/reclaims/" + applicationId);
        return domainService.reclaim(applicationId, request.getReason(), request.getProofId(), reclaimedBy);
    }

    /** Copies the result and both inputs onto the application, so later changes to either never rewrite history. */
    private void freezeMatch(AdoptionApplication application, MatchingQueryPort.Evaluation evaluation) {
        MatchResult result = evaluation.result();
        application.setMatchLevel(result.level().name());
        application.setMatchScore(result.score() == null ? null : BigDecimal.valueOf(result.score()));
        application.setMatchCertainty(result.certainty().name());
        application.setMatchWeightsVersion(result.weightsVersion());
        application.setMatchWarnings(result.hasWarnings()
                ? result.warnings().stream().map(w -> w.code().name()).collect(Collectors.joining(","))
                : null);
        application.setMatchRank(result.hasWarnings() ? BigDecimal.valueOf(-2) : BigDecimal.valueOf(result.score()));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("species", evaluation.species());
        snapshot.put("adopterProfile", evaluation.adopterProfile());
        snapshot.put("petTraits", evaluation.petTraits());
        snapshot.put("result", result);
        try {
            application.setMatchSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            // The columns above already hold the outcome; losing the detail must not lose the application.
            log.warn("Could not serialize the match snapshot for pet {}: {}", application.getPetId(), e.getMessage());
        }
    }
}
