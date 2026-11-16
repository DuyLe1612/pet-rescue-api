package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.AdoptionApplicationRepository;
import com.uit.petrescueapi.domain.repository.FollowupRepository;
import com.uit.petrescueapi.domain.repository.FollowupRepository.AlertRow;
import com.uit.petrescueapi.domain.repository.FollowupRepository.Row;
import com.uit.petrescueapi.domain.valueobject.FollowupSeverity;
import com.uit.petrescueapi.domain.repository.FollowupRepository.Outcome;
import com.uit.petrescueapi.domain.valueobject.AcquisitionType;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.domain.valueobject.FollowupAnswers;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Follow-up after an adoption. The answers never touch reputation: if honesty were punished,
 * adopters would stop being honest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FollowupDomainService {

    /** A milestone sent and not yet reminded is reminded once after this long. */
    private static final Duration REMINDER_AFTER = Duration.ofHours(48);

    private final FollowupRepository repository;
    private final AdoptionApplicationRepository applications;
    private final AccessControlDomainService accessControl;
    private final NotificationDomainService notifications;
    private final PetDomainService pets;

    private final SecureRandom random = new SecureRandom();

    /** Offsets from completion to each milestone: day 2, week 2, month 3, month 6. */
    @Value("${app.followup.offsets:P2D,P14D,P90D,P180D}")
    private List<Duration> offsets;

    /** How long a link works after it is sent. */
    @Value("${app.followup.link-validity:P14D}")
    private Duration linkValidity;

    /** A milestone as sent: the raw token exists only here, for the email, and is never stored. */
    public record Delivery(Row followup, String token, String email, String adopterName) {
    }

    public void scheduleFor(UUID applicationId, LocalDateTime completedAt) {
        repository.schedule(applicationId, completedAt, offsets);
    }

    public void cancelOpen(UUID applicationId) {
        repository.cancelOpen(applicationId);
    }

    // ── Sending ─────────────────────────────────────

    public List<UUID> findDue(int limit) {
        repository.expireSuperseded(LocalDateTime.now());
        return repository.findDueIds(LocalDateTime.now(), limit);
    }

    /** Claims one due milestone. Empty when another instance took it or it is no longer due. */
    public java.util.Optional<Delivery> dispatch(UUID followupId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        return repository.claim(followupId, hash(token), now, now.plus(linkValidity)).map(row -> {
            notifications.notifyUsers(List.of(row.adopterId()), NotificationType.FOLLOWUP_DUE,
                    "Bé " + row.petName() + " thế nào rồi?", "Cho chúng mình biết bé đang ra sao, chỉ mất chưa đến một phút.",
                    Map.of("followupId", String.valueOf(row.followupId()), "applicationId", String.valueOf(row.applicationId())));
            FollowupRepository.Contact contact = repository.findContact(row.adopterId()).orElse(null);
            return new Delivery(row, token, contact == null ? null : contact.email(), contact == null ? null : contact.name());
        });
    }

    /** Reminds, once, the adopters who have not answered after {@link #REMINDER_AFTER}. */
    public int remind() {
        LocalDateTime now = LocalDateTime.now();
        List<Row> due = repository.claimReminders(now, now.minus(REMINDER_AFTER));
        for (Row row : due) {
            notifications.notifyUsers(List.of(row.adopterId()), NotificationType.FOLLOWUP_REMINDER,
                    "Bé " + row.petName() + " vẫn đang chờ tin của bạn",
                    "Nếu bạn cần hỗ trợ, hãy trả lời hoặc nhắn cho tổ chức.",
                    Map.of("followupId", String.valueOf(row.followupId()), "applicationId", String.valueOf(row.applicationId())));
        }
        return due.size();
    }

    /** Closes milestones nobody answered. Not answering is counted as such, never as "fine". */
    public int expire() {
        List<Row> expired = repository.expireOverdue(LocalDateTime.now());
        for (Row row : expired) {
            // No answer to the first question may mean the pet is not doing well: the organization should call.
            if ("DAY_2".equals(row.milestone())) {
                raise(row, FollowupSeverity.YELLOW, List.of("NO_RESPONSE"));
            }
        }
        return expired.size();
    }

    /** Opens work for the organization; a red one also tells every owner and staff member now. */
    private void raise(Row row, FollowupSeverity severity, List<String> reasons) {
        repository.createAlert(row.followupId(), row.applicationId(), row.organizationId(), severity.name(), reasons, null);
        if (severity == FollowupSeverity.RED) {
            notifications.notifyOrganizationHandlers(row.organizationId(), NotificationType.FOLLOWUP_ALERT,
                    "Cần liên hệ người nhận nuôi bé " + row.petName(), "Câu trả lời sau nhận nuôi có điều đáng lo: " + String.join(", ", reasons),
                    Map.of("applicationId", String.valueOf(row.applicationId()), "petId", String.valueOf(row.petId())));
        }
    }

    // ── Answering ───────────────────────────────────

    /** What the public form needs: the pet's name and which milestone. Nothing personal. */
    @Transactional(readOnly = true)
    public Row formFor(String token) {
        Row row = repository.findByTokenHash(hash(token)).orElseThrow(FollowupDomainService::invalidLink);
        requireOpen(row);
        return row;
    }

    public Row respondWithToken(String token, FollowupAnswers answers) {
        Row row = repository.findByTokenHash(hash(token)).orElseThrow(FollowupDomainService::invalidLink);
        return record(row, answers);
    }

    public Row respond(UUID followupId, UUID userId, FollowupAnswers answers) {
        Row row = repository.findById(followupId).orElseThrow(() -> new ResourceNotFoundException("Follow-up", "id", followupId));
        if (!row.adopterId().equals(userId)) {
            throw new ResourceNotFoundException("Follow-up", "id", followupId);
        }
        return record(row, answers);
    }

    private Row record(Row row, FollowupAnswers answers) {
        requireOpen(row);
        FollowupRules.Assessment assessment = FollowupRules.assess(answers);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("stillHasPet", answers.stillHasPet());
        stored.put("wellbeing", answers.wellbeing());
        stored.put("fear", answers.fear());
        stored.put("incident", answers.incident());
        stored.put("support", answers.support());
        stored.put("note", answers.note() == null || answers.note().isBlank() ? null : answers.note().trim());
        stored.put("reasons", assessment.reasons());
        // Conditional on the row still being open, so two submissions of one link cannot both count.
        if (!repository.recordAnswers(row.followupId(), stored, answers.stillHasPet(), assessment.severity().name(), LocalDateTime.now())) {
            throw invalidLink();
        }
        if (assessment.severity() != FollowupSeverity.GREEN) {
            raise(row, assessment.severity(), assessment.reasons());
        }
        return repository.findById(row.followupId()).orElseThrow();
    }

    private static void requireOpen(Row row) {
        if (!"SENT".equals(row.status()) || row.expiresAt() == null || !row.expiresAt().isAfter(LocalDateTime.now())) {
            throw invalidLink();
        }
    }

    /** One message for every reason, so a link cannot be probed. */
    private static BusinessException invalidLink() {
        return new BusinessException("This link is no longer valid", "FOLLOWUP_LINK_INVALID");
    }

    // ── Reading ─────────────────────────────────────

    /** The adopter, the organization that handed the pet over, and admins; nobody else. */
    @Transactional(readOnly = true)
    public List<Row> timeline(UUID applicationId, UUID viewerId) {
        var app = applications.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Adoption application", "id", applicationId));
        boolean adopter = app.getApplicantId().equals(viewerId);
        if (!adopter && !accessControl.isAdmin(viewerId) && !repository.isOrganizationHandler(app.getOrganizationId(), viewerId)) {
            throw new ForbiddenException("You cannot read the follow-up of this adoption");
        }
        return repository.findByApplication(applicationId);
    }

    // ── The adopter asks for help, or can no longer keep the pet ──

    /** "I need support": work for the organization, any time, without waiting for a milestone. */
    public void requestSupport(UUID applicationId, UUID userId, String note) {
        AdoptionApplication app = adopterApplication(applicationId, userId);
        if (!repository.hasUnresolvedAlert(applicationId, "SUPPORT_REQUESTED")) {
            repository.createAlert(null, applicationId, app.getOrganizationId(), FollowupSeverity.YELLOW.name(),
                    List.of("SUPPORT_REQUESTED"), blankToNull(note));
        }
    }

    /**
     * "I cannot keep the pet any more": red work, and the organization is told now. No reputation is
     * lost: a person who is punished for saying so would stay silent and the animal would be abandoned.
     */
    public void requestReturn(UUID applicationId, UUID userId, String reasonCode, String note) {
        AdoptionApplication app = adopterApplication(applicationId, userId);
        if (repository.hasUnresolvedAlert(applicationId, "RETURN_REQUESTED")) {
            return;
        }
        repository.createAlert(null, applicationId, app.getOrganizationId(), FollowupSeverity.RED.name(),
                List.of("RETURN_REQUESTED", reasonCode), blankToNull(note));
        notifications.notifyOrganizationHandlers(app.getOrganizationId(), NotificationType.FOLLOWUP_ALERT,
                "Người nhận nuôi muốn trả bé lại", "Cần liên hệ sớm để hỗ trợ hoặc sắp xếp đón bé về.",
                Map.of("applicationId", String.valueOf(applicationId), "petId", String.valueOf(app.getPetId())));
    }

    /** Turning it off stops every question still to come; turning it on again restores those not yet due. */
    public void setPreference(UUID applicationId, UUID userId, boolean enabled) {
        adopterApplication(applicationId, userId);
        repository.setOptOut(applicationId, !enabled);
        if (!enabled) {
            repository.cancelOpen(applicationId);
        } else if (repository.findOutcome(applicationId).isEmpty()) {
            repository.reviveFuture(applicationId, LocalDateTime.now());
        }
    }

    private AdoptionApplication adopterApplication(UUID applicationId, UUID userId) {
        AdoptionApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Adoption application", "id", applicationId));
        if (!app.getApplicantId().equals(userId)) {
            throw new ResourceNotFoundException("Adoption application", "id", applicationId);
        }
        if (app.getStatus() != AdoptionApplicationStatus.COMPLETED || repository.findOutcome(applicationId).isPresent()) {
            throw new ConflictException("This adoption is not active", "ADOPTION_NOT_ACTIVE");
        }
        return app;
    }

    // ── How an adoption ended ───────────────────────

    /**
     * The organization confirms the result. A pet that goes back becomes the organization's again
     * (FOSTERING) without costing the adopter reputation; a lost or dead pet only changes the pet's
     * status. Whatever the outcome, no more questions are sent.
     */
    public Outcome recordOutcome(UUID applicationId, UUID userId, String type, String reasonCode, String note) {
        AdoptionApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Adoption application", "id", applicationId));
        if (!accessControl.isAdmin(userId) && !repository.isOrganizationHandler(app.getOrganizationId(), userId)) {
            throw new ForbiddenException("Only the organization that handed the pet over can record the outcome");
        }
        if (app.getStatus() != AdoptionApplicationStatus.COMPLETED || repository.findOutcome(applicationId).isPresent()) {
            throw new ConflictException("This adoption already ended or never completed", "ADOPTION_NOT_ACTIVE");
        }
        switch (type) {
            case "RETURNED_BY_ADOPTER" -> {
                pets.transferOwnership(app.getPetId(), "ORGANIZATION", app.getOrganizationId(), AcquisitionType.ORG_INTAKE, null);
                pets.changeStatus(app.getPetId(), PetStatus.FOSTERING);
            }
            case "LOST" -> pets.changeStatus(app.getPetId(), PetStatus.LOST);
            case "DECEASED" -> pets.changeStatus(app.getPetId(), PetStatus.DECEASED);
            case "REHOMED" -> { }
            default -> throw new BusinessException("Unknown outcome " + type, "FOLLOWUP_OUTCOME_INVALID");
        }
        UUID initiatedBy = "RETURNED_BY_ADOPTER".equals(type) ? app.getApplicantId() : userId;
        finish(app, type, reasonCode, initiatedBy, note, "RETURNED_BY_ADOPTER".equals(type) ? "RETURNED" : "OTHER");
        notifications.notifyUsers(List.of(app.getApplicantId()), NotificationType.ADOPTION_RECLAIMED,
                "Đã ghi nhận kết quả nhận nuôi", "Cảm ơn bạn đã cho bé một mái nhà.",
                Map.of("applicationId", String.valueOf(applicationId), "petId", String.valueOf(app.getPetId())));
        return repository.findOutcome(applicationId).orElseThrow();
    }

    /** A reclaim for a violation is also an outcome, so the numbers cover every way an adoption can end. */
    public void recordReclaim(AdoptionApplication app, UUID reclaimedBy) {
        finish(app, "RECLAIMED", "OTHER", reclaimedBy, null, "RETURNED");
    }

    private void finish(AdoptionApplication app, String type, String reasonCode, UUID initiatedBy, String note, String alertResolution) {
        LocalDateTime now = LocalDateTime.now();
        repository.insertOutcome(app.getApplicationId(), app.getPetId(), type, reasonCode, initiatedBy, now, blankToNull(note));
        repository.cancelOpen(app.getApplicationId());
        repository.resolveOpenAlerts(app.getApplicationId(), alertResolution, now);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    // ── The organization's work ─────────────────────

    /** Owners and staff see their own organization's alerts, admins every organization's. */
    @Transactional(readOnly = true)
    public List<AlertRow> alerts(UUID userId, String status) {
        if (accessControl.isAdmin(userId)) {
            return repository.findAlerts(null, status, 100);
        }
        UUID organizationId = repository.findHandlerOrganizationId(userId)
                .orElseThrow(() -> new ForbiddenException("Only an organization's owners and staff have follow-up work"));
        return repository.findAlerts(organizationId, status, 100);
    }

    /** Take it, note it, close it. The first time it leaves OPEN is what the response time is counted from. */
    public AlertRow updateAlert(UUID alertId, UUID userId, String status, String note, String resolution) {
        AlertRow alert = repository.findAlert(alertId).orElseThrow(() -> new ResourceNotFoundException("Follow-up alert", "id", alertId));
        if (!accessControl.isAdmin(userId) && !repository.isOrganizationHandler(alert.organizationId(), userId)) {
            throw new ForbiddenException("This alert belongs to another organization");
        }
        if ("RESOLVED".equals(status) && (resolution == null || resolution.isBlank())) {
            throw new BusinessException("Say how it was resolved", "FOLLOWUP_ALERT_RESOLUTION_REQUIRED");
        }
        repository.updateAlert(alertId, status, userId, note == null || note.isBlank() ? null : note.trim(), resolution, LocalDateTime.now());
        return repository.findAlert(alertId).orElseThrow();
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
