package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.adoption.CreateAdoptionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.DecisionRequestDto;
import com.uit.petrescueapi.application.dto.adoption.ReclaimPetRequestDto;
import com.uit.petrescueapi.application.port.command.AdoptionCommandPort;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.out.PushNotificationPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.ReclaimLog;
import com.uit.petrescueapi.domain.service.AdoptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final PushNotificationPort pushNotificationPort;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public AdoptionApplication submit(CreateAdoptionRequestDto cmd, UUID applicantId) {
        log.debug("Command: submit adoption application for pet {} by user {}", cmd.getPetId(), applicantId);
        mediaQueryPort.findById(cmd.getSignatureMediaId());
        mediaCommandPort.confirmUpload(cmd.getSignatureMediaId(), "adoptions/signatures/" + applicantId);
        AdoptionApplication application = AdoptionApplication.builder()
                .petId(cmd.getPetId())
                .organizationId(cmd.getOrganizationId())
                .applicantId(applicantId)
                .experience(cmd.getExperience())
                .housingCondition(cmd.getHousingCondition())
                .liveCondition(cmd.getLiveCondition())
                .signatureMediaId(cmd.getSignatureMediaId())
                .build();
        AdoptionApplication submitted = domainService.submit(application);
        notifyOrganizationMembers(submitted);
        return submitted;
    }

    @Override
    public AdoptionApplication approve(UUID applicationId, DecisionRequestDto decision, UUID decidedBy) {
        log.debug("Command: approve adoption application {}", applicationId);
        return domainService.approve(applicationId, decidedBy, decision.getReadyAt());
    }

    @Override
    public AdoptionApplication reject(UUID applicationId, DecisionRequestDto decision, UUID decidedBy) {
        log.debug("Command: reject adoption application {}", applicationId);
        return domainService.reject(applicationId, decidedBy, decision.getRejectReason());
    }

    @Override
    public AdoptionApplication cancel(UUID applicationId) {
        log.debug("Command: cancel adoption application {}", applicationId);
        return domainService.cancel(applicationId);
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

    private void notifyOrganizationMembers(AdoptionApplication application) {
        List<String> tokens = jdbcTemplate.queryForList("""
                SELECT u.expo_push_token
                FROM organization_members om
                JOIN users u ON om.user_id = u.user_id
                WHERE om.organization_id = ?
                  AND om.status = 'ACTIVE'
                  AND om.role IN ('OWNER', 'STAFF')
                  AND u.expo_push_token IS NOT NULL
                  AND u.is_deleted = false
                """, String.class, application.getOrganizationId());
        if (tokens.isEmpty()) {
            return;
        }
        pushNotificationPort.sendPushToTokens(
                tokens,
                "New adoption application",
                "A user submitted an adoption application.",
                Map.of(
                        "type", "ADOPTION_APPLICATION_SUBMITTED",
                        "applicationId", application.getApplicationId().toString(),
                        "petId", application.getPetId().toString()
                )
        );
    }
}
