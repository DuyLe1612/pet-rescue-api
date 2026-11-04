package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.*;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescueCompletionStatus;
import com.uit.petrescueapi.domain.valueobject.RescueOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service encapsulating RescueCase business rules.
 *
 * Rules:
 *  - New rescue cases always start with status REPORTED.
 *  - Status transitions follow: REPORTED -> IN_PROGRESS -> RESCUED -> CLOSED.
 *  - IN_PROGRESS returns to REPORTED when nobody is working on the case any more.
 *  - Deletion is soft-delete.
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RescueCaseDomainService {

    private final RescueCaseRepository rescueCaseRepository;
    private final RescueCaseCompletionRepository rescueCaseCompletionRepository;
    private final VisualCodeRepository visualCodeRepository;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final RescueClaimDomainService claimService;
    private final NotificationDomainService notifications;
    private final ReputationDomainService reputation;

    /** Score for a rescue another person confirmed; the limit stops two accounts farming it. */
    private static final int RESCUE_REPUTATION = 30;
    private static final int RESCUE_REPUTATION_DAILY_LIMIT = 3;

    // ── Status-transition matrix ───────────────────
    private static final Map<RescueCaseStatus, Set<RescueCaseStatus>> ALLOWED_TRANSITIONS = Map.of(
            RescueCaseStatus.REPORTED,    Set.of(RescueCaseStatus.IN_PROGRESS, RescueCaseStatus.RESCUED, RescueCaseStatus.CLOSED),
            RescueCaseStatus.IN_PROGRESS, Set.of(RescueCaseStatus.RESCUED, RescueCaseStatus.CLOSED),
            RescueCaseStatus.RESCUED,     Set.of(RescueCaseStatus.CLOSED),
            RescueCaseStatus.CLOSED,      Set.of()
    );

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public RescueCase findById(UUID caseId) {
        return rescueCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", "caseId", caseId));
    }

    @Transactional(readOnly = true)
    public Optional<RescueCaseCompletion> findCompletionById(UUID completionId) {
        return rescueCaseCompletionRepository.findById(completionId);
    }
    // ── Commands ────────────────────────────────────

    /**
     * Report a new rescue case.
     * Sets the id, status to REPORTED, and reportedAt timestamp.
     */
    public RescueCase report(RescueCase rescueCase) {
        log.info("Reporting new rescue case");
//        rescueCase.setCaseId(UUID.randomUUID());
        rescueCase.setCaseCode(visualCodeRepository.nextRescueCaseCode());
        rescueCase.setStatus(RescueCaseStatus.REPORTED);
        rescueCase.setReportedAt(LocalDateTime.now());
        rescueCase.setCreatedAt(LocalDateTime.now());
        return rescueCaseRepository.save(rescueCase);
    }

    /**
     * Stores a completion report. Staff of the organization holding the case need no second opinion;
     * anyone else's report waits for the reporter, an organization or an admin.
     */
    public RescueCaseCompletion reportCompletion(RescueCaseCompletion completion) {
        UUID caseId = completion.getCaseId();
        UUID submitter = completion.getSubmittedBy();
        RescueCase rescueCase = rescueCaseRepository.findByIdForUpdate(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", "caseId", caseId));
        requireOpen(rescueCase);
        if (rescueCaseCompletionRepository.findOpenByCaseId(caseId).isPresent()) {
            throw new ConflictException("A completion report already exists for this case", "RESCUE_COMPLETION_EXISTS");
        }
        LocalDateTime now = LocalDateTime.now();
        if (completion.getRescuedAt() == null) {
            completion.setRescuedAt(now);
        } else if (completion.getRescuedAt().isAfter(now)) {
            throw new BusinessException("Rescue time cannot be in the future", "RESCUE_TIME_INVALID");
        }
        if (completion.getOutcome() == null) {
            completion.setOutcome(RescueOutcome.TEMP_HOLDING);
        }
        completion.setStatus(RescueCompletionStatus.PENDING);
        completion.setCreatedAt(now);

        RescueCaseClaim claim = claimService.settleClaimForCompletion(rescueCase, submitter);
        RescueCase current = findById(caseId);      // settling may have assigned the organization

        if (claim.getOrganizationId() != null && claim.getOrganizationId().equals(current.getOrganizationId())) {
            return approve(current, completion, submitter, false);
        }

        RescueCaseCompletion saved = rescueCaseCompletionRepository.save(completion);
        if (current.getStatus() == RescueCaseStatus.REPORTED) {
            setStatus(current, RescueCaseStatus.IN_PROGRESS);
        }
        notifyReviewers(current, saved);
        log.info("Completion {} submitted for rescue case {}", saved.getCompletionId(), caseId);
        return saved;
    }

    /**
     * Partial update of an existing rescue case.
     */
    public RescueCase update(UUID caseId, RescueCase patch) {
        log.debug("Updating rescue case {}", caseId);
        RescueCase existing = findById(caseId);
        requireOpen(existing);
        boolean moved = (patch.getLatitude() != null && !patch.getLatitude().equals(existing.getLatitude()))
                || (patch.getLongitude() != null && !patch.getLongitude().equals(existing.getLongitude()));
        applyUpdates(existing, patch);
        existing.setUpdatedAt(LocalDateTime.now());
        RescueCase saved = rescueCaseRepository.save(existing);
        if (moved) {
            notifications.notifyUsers(claimService.findActiveByCase(caseId).stream().map(RescueCaseClaim::getUserId).toList(),
                    NotificationType.RESCUE_CASE_MOVED, "Ca " + existing.getCaseCode() + " đổi vị trí",
                    "Kiểm tra lại vị trí trước khi đến.", caseData(caseId));
        }
        return saved;
    }

    /**
     * Change the status of a rescue case with transition validation.
     */
    public RescueCase changeStatus(UUID caseId, RescueCaseStatus newStatus) {
        return changeStatus(caseId, newStatus, null, null);
    }

    public RescueCase changeStatus(UUID caseId, RescueCaseStatus newStatus, String reason, UUID actorId) {
        log.info("Changing rescue case {} status to {}", caseId, newStatus);
        RescueCase rescueCase = findById(caseId);
        RescueCaseStatus current = rescueCase.getStatus();

        Set<RescueCaseStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition rescue case status from %s to %s", current, newStatus));
        }

        boolean ended = newStatus == RescueCaseStatus.CLOSED || newStatus == RescueCaseStatus.RESCUED;
        if (ended && rescueCaseCompletionRepository.findOpenByCaseId(caseId)
                .filter(c -> c.getStatus() != RescueCompletionStatus.APPROVED).isPresent()) {
            throw new ConflictException("Settle the pending completion report first", "RESCUE_COMPLETION_PENDING");
        }

        if (reason != null) {
            rescueCase.setResolutionNote(reason);
        }
        RescueCase saved = setStatus(rescueCase, newStatus);
        if (ended) {
            claimService.endAllActive(saved, actorId);
        }
        return saved;
    }
    public RescueCaseCompletion approveCompletion(UUID completionId, UUID userId) {
        log.debug("Approving rescue case completion {} by user {}", completionId, userId);
        RescueCaseCompletion completion = findCompletion(completionId);
        requireReviewable(completion);
        RescueCase rescueCase = findById(completion.getCaseId());
        requireOpen(rescueCase);
        return approve(rescueCase, completion, userId, true);
    }

    /** The reporter says the rescue really happened; they cannot vouch for their own report. */
    public RescueCaseCompletion confirmCompletion(UUID completionId, UUID reporterId) {
        RescueCaseCompletion completion = findCompletion(completionId);
        RescueCase rescueCase = findById(completion.getCaseId());
        requireReporterOfOtherPersonsReport(rescueCase, completion, reporterId);
        if (completion.getStatus() != RescueCompletionStatus.PENDING) {
            throw new ConflictException("This report is no longer waiting for you", "RESCUE_COMPLETION_NOT_PENDING");
        }
        requireOpen(rescueCase);
        return approve(rescueCase, completion, reporterId, true);
    }

    /** The reporter disagrees. That alone does not reject the report: an admin looks at both sides. */
    public RescueCaseCompletion disputeCompletion(UUID completionId, String reason, UUID reporterId) {
        RescueCaseCompletion completion = findCompletion(completionId);
        RescueCase rescueCase = findById(completion.getCaseId());
        requireReporterOfOtherPersonsReport(rescueCase, completion, reporterId);
        if (completion.getStatus() != RescueCompletionStatus.PENDING) {
            throw new ConflictException("This report is no longer waiting for you", "RESCUE_COMPLETION_NOT_PENDING");
        }
        completion.setStatus(RescueCompletionStatus.DISPUTED);
        completion.setRejectReason(reason);
        completion.setUpdatedAt(LocalDateTime.now());
        RescueCaseCompletion saved = rescueCaseCompletionRepository.save(completion);
        notifications.notifyAdmins(NotificationType.RESCUE_COMPLETION_NEEDS_REVIEW,
                "Tranh chấp ở ca " + rescueCase.getCaseCode(), "Người báo không đồng ý với báo cáo hoàn tất.",
                completionData(saved));
        return saved;
    }

    /** A rejected report stops blocking the case, which reopens if nobody else is on it. */
    public RescueCaseCompletion rejectCompletion(UUID completionId, String reason, UUID userId) {
        RescueCaseCompletion completion = findCompletion(completionId);
        requireReviewable(completion);
        completion.setStatus(RescueCompletionStatus.REJECTED);
        completion.setRejectReason(reason);
        completion.setUpdatedAt(LocalDateTime.now());
        RescueCaseCompletion saved = rescueCaseCompletionRepository.save(completion);
        RescueCase rescueCase = findById(completion.getCaseId());
        claimService.reopenIfIdle(rescueCase, false);
        notifications.notifyUsers(
                java.util.stream.Stream.of(completion.getSubmittedBy(), rescueCase.getReportedBy()).filter(java.util.Objects::nonNull).toList(),
                NotificationType.RESCUE_COMPLETION_REJECTED, "Báo cáo ca " + rescueCase.getCaseCode() + " bị từ chối",
                reason, completionData(saved));
        log.info("Completion {} rejected by {}", completionId, userId);
        return saved;
    }

    /** Hands an overdue report to the admins. Marked so they are told once. */
    public void escalateCompletion(UUID completionId) {
        RescueCaseCompletion completion = findCompletion(completionId);
        if (completion.getStatus() != RescueCompletionStatus.PENDING || completion.getEscalatedAt() != null) {
            return;
        }
        completion.setEscalatedAt(LocalDateTime.now());
        rescueCaseCompletionRepository.save(completion);
        notifications.notifyAdmins(NotificationType.RESCUE_COMPLETION_NEEDS_REVIEW,
                "Báo cáo hoàn tất quá hạn xác nhận", "Chưa ai xác nhận báo cáo này.", completionData(completion));
    }

    @Transactional(readOnly = true)
    public java.util.List<RescueCaseCompletion> findCompletionsToEscalate(LocalDateTime createdBefore, int limit) {
        return rescueCaseCompletionRepository.findPendingToEscalate(createdBefore, limit);
    }

    @Transactional(readOnly = true)
    public Optional<RescueCaseCompletion> findOpenCompletion(UUID caseId) {
        return rescueCaseCompletionRepository.findOpenByCaseId(caseId);
    }

    /** Records the pet that came out of this case; once only. */
    public RescueCase linkPet(UUID caseId, UUID petId) {
        RescueCase rescueCase = findById(caseId);
        if (rescueCase.getPetId() != null) {
            throw new ConflictException("This case already has a pet record", "RESCUE_CASE_HAS_PET");
        }
        rescueCase.setPetId(petId);
        rescueCase.setUpdatedAt(LocalDateTime.now());
        return rescueCaseRepository.save(rescueCase);
    }

    // ── Private helpers ─────────────────────────────

    private RescueCaseCompletion approve(RescueCase rescueCase, RescueCaseCompletion completion, UUID verifierId, boolean reward) {
        LocalDateTime now = LocalDateTime.now();
        completion.setStatus(RescueCompletionStatus.APPROVED);
        completion.setVerifiedBy(verifierId);
        completion.setVerifiedAt(now);
        completion.setUpdatedAt(now);
        RescueCaseCompletion saved = rescueCaseCompletionRepository.save(completion);

        boolean closes = completion.getOutcome() != null && completion.getOutcome().closesCase();
        rescueCase.setNeedsHelp(false);
        RescueCase updated = setStatus(rescueCase, closes ? RescueCaseStatus.CLOSED : RescueCaseStatus.RESCUED);
        claimService.endAllActive(updated, completion.getSubmittedBy());

        if (reward && completion.getSubmittedBy() != null) {
            reputation.adjustWithDailyLimit(completion.getSubmittedBy(), RESCUE_REPUTATION, "RESCUE_COMPLETED",
                    "RESCUE_CASE", rescueCase.getCaseId(), verifierId, RESCUE_REPUTATION_DAILY_LIMIT);
        }
        notifications.notifyUsers(
                java.util.stream.Stream.of(completion.getSubmittedBy(), rescueCase.getReportedBy())
                        .filter(java.util.Objects::nonNull).filter(id -> !id.equals(verifierId)).toList(),
                NotificationType.RESCUE_COMPLETION_APPROVED, "Ca " + rescueCase.getCaseCode() + " đã được xác nhận",
                "Báo cáo hoàn tất đã được xác nhận.", completionData(saved));
        return saved;
    }

    /** Tells whoever can settle the report; with no second person around, that is the admins. */
    private void notifyReviewers(RescueCase rescueCase, RescueCaseCompletion completion) {
        String title = "Có báo cáo hoàn tất ca " + rescueCase.getCaseCode();
        String body = "Xem ảnh và xác nhận giúp nhé.";
        Map<String, String> data = completionData(completion);
        boolean reporterCanConfirm = rescueCase.getReportedBy() != null
                && !rescueCase.getReportedBy().equals(completion.getSubmittedBy());
        if (reporterCanConfirm) {
            notifications.notifyUsers(java.util.List.of(rescueCase.getReportedBy()), NotificationType.RESCUE_COMPLETION_SUBMITTED, title, body, data);
        }
        if (rescueCase.getOrganizationId() != null) {
            notifications.notifyOrganizationHandlers(rescueCase.getOrganizationId(), NotificationType.RESCUE_COMPLETION_SUBMITTED, title, body, data);
        } else if (!reporterCanConfirm) {
            notifications.notifyAdmins(NotificationType.RESCUE_COMPLETION_NEEDS_REVIEW, title, body, data);
        }
    }

    private RescueCase setStatus(RescueCase rescueCase, RescueCaseStatus status) {
        LocalDateTime now = LocalDateTime.now();
        rescueCase.setStatus(status);
        rescueCase.setUpdatedAt(now);
        if (status == RescueCaseStatus.CLOSED || status == RescueCaseStatus.RESCUED) {
            rescueCase.setResolvedAt(now);
        }
        RescueCase saved = rescueCaseRepository.save(rescueCase);
        events.publishEvent(new com.uit.petrescueapi.domain.event.RescueCaseStatusChangedEvent(rescueCase.getCaseId(), status.name()));
        return saved;
    }

    private RescueCaseCompletion findCompletion(UUID completionId) {
        return findCompletionById(completionId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCaseCompletion", "completionId", completionId));
    }

    private static void requireOpen(RescueCase rescueCase) {
        if (rescueCase.getStatus() != RescueCaseStatus.REPORTED && rescueCase.getStatus() != RescueCaseStatus.IN_PROGRESS) {
            throw new ConflictException("This case has already ended", "RESCUE_CASE_NOT_OPEN");
        }
    }

    private static void requireReviewable(RescueCaseCompletion completion) {
        if (completion.getStatus() != RescueCompletionStatus.PENDING && completion.getStatus() != RescueCompletionStatus.DISPUTED) {
            throw new BusinessException("Completion already " + completion.getStatus(), "RESCUE_COMPLETION_NOT_PENDING");
        }
    }

    private static void requireReporterOfOtherPersonsReport(RescueCase rescueCase, RescueCaseCompletion completion, UUID userId) {
        if (userId == null || !userId.equals(rescueCase.getReportedBy())) {
            throw new ForbiddenException("Only the reporter of this case can do this");
        }
        if (userId.equals(completion.getSubmittedBy())) {
            throw new ForbiddenException("You cannot vouch for your own report");
        }
    }

    private static Map<String, String> caseData(UUID caseId) {
        return Map.of("caseId", caseId.toString());
    }

    private static Map<String, String> completionData(RescueCaseCompletion completion) {
        return Map.of("caseId", completion.getCaseId().toString(), "completionId", completion.getCompletionId().toString());
    }

    private void applyUpdates(RescueCase target, RescueCase source) {
        if (source.getSpecies() != null)      target.setSpecies(source.getSpecies());
        if (source.getColor() != null)        target.setColor(source.getColor());
        if (source.getSize() != null)         target.setSize(source.getSize());
        if (source.getPriority() != null)     target.setPriority(source.getPriority());
        if (source.getDescription() != null)  target.setDescription(source.getDescription());
        if (source.getLatitude() != null)     target.setLatitude(source.getLatitude());
        if (source.getLongitude() != null)    target.setLongitude(source.getLongitude());
        if (source.getLocationText() != null) target.setLocationText(source.getLocationText());
        if (source.getProvinceCode() != null) target.setProvinceCode(source.getProvinceCode());
        if (source.getWardCode() != null)     target.setWardCode(source.getWardCode());
        if (source.getOrganizationId() != null) target.setOrganizationId(source.getOrganizationId());
    }


}
