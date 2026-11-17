package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.ReclaimLog;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.exception.UnprocessableEntityException;
import com.uit.petrescueapi.domain.repository.AdoptionApplicationRepository;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.repository.ReclaimLogRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service encapsulating AdoptionApplication business rules.
 *
 * Rules:
 *  - New applications always start with status PENDING.
 *  - Approval: PENDING -> APPROVED (requires decidedBy).
 *  - Rejection: PENDING -> REJECTED (requires decidedBy).
 *  - Cancellation: PENDING -> CANCELED.
 *  - Completion: APPROVED -> COMPLETED (transfers ownership).
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdoptionDomainService {

    private final AdoptionApplicationRepository applicationRepository;
    private final PetDomainService petDomainService;
    private final VisualCodeRepository visualCodeRepository;
    private final OrganizationDomainService organizationDomainService;
    private final AccessControlDomainService accessControl;
    private final PetCurrentOwnerRepository currentOwnerRepository;
    private final ReputationDomainService reputationDomainService;
    private final ReclaimLogRepository reclaimLogRepository;
    private final NotificationDomainService notifications;

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public AdoptionApplication findById(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("AdoptionApplication", "applicationId", applicationId));
    }

    // ── Commands ────────────────────────────────────

    /**
     * Submit a new adoption application.
     * Sets the id, status to PENDING, and createdAt timestamp.
     */
    public AdoptionApplication submit(AdoptionApplication application) {
        log.info("Submitting adoption application for pet {}", application.getPetId());
        Pet pet = petDomainService.findById(application.getPetId());
        if (pet.getStatus() != PetStatus.AVAILABLE) {
            throw new ConflictException("Pet is not available for adoption", "PET_NOT_AVAILABLE");
        }

        long pendingCount = applicationRepository.countByApplicantIdAndStatus(
                application.getApplicantId(),
                AdoptionApplicationStatus.PENDING
        );
        if (pendingCount >= 2) {
            throw new UnprocessableEntityException(
                    "User already has 2 pending adoption applications",
                    "ADOPTION_SPAM_LIMIT"
            );
        }

        // After the hard business rules above: a warning never blocks, but the applicant must have read it.
        if (application.hasMatchWarnings() && !application.isWarningsAcknowledged()) {
            throw new UnprocessableEntityException(
                    "This pet has compatibility warnings for you. Confirm that you have read them to apply.",
                    "MATCH_WARNING_NOT_ACKNOWLEDGED");
        }

        // Always derived from the pet: a client-supplied value would let a caller route
        // the application to their own organization and approve it themselves.
        application.setOrganizationId(resolvePetOrganizationOwner(application.getPetId()));

        application.setApplicationId(UUID.randomUUID());
        application.setAdoptionCode(visualCodeRepository.nextAdoptionCode());
        application.setStatus(AdoptionApplicationStatus.PENDING);
        application.setCreatedAt(LocalDateTime.now());
        AdoptionApplication saved = applicationRepository.save(application);
        notifications.notifyOrganizationHandlers(saved.getOrganizationId(), NotificationType.ADOPTION_SUBMITTED,
                "Có đơn nhận nuôi mới " + code(saved), "Một người dùng vừa nộp đơn nhận nuôi. Hãy xem và phản hồi.", data(saved));
        return saved;
    }

    /**
     * Approve an adoption application.
     * Validates the current status is PENDING before transitioning to APPROVED.
     */
    public AdoptionApplication approve(UUID applicationId, UUID decidedBy, LocalDateTime readyAt) {
        return approve(applicationId, decidedBy, readyAt, null);
    }

    public AdoptionApplication approve(UUID applicationId, UUID decidedBy, LocalDateTime readyAt, String reasonCode) {
        log.info("Approving adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        accessControl.requireAdoptionDecider(app.getOrganizationId(), decidedBy);
        validateStatus(app, AdoptionApplicationStatus.PENDING, AdoptionApplicationStatus.APPROVED);

        if (readyAt == null) {
            throw new BusinessException("readyAt is required when approving adoption", "ADOPTION_READY_AT_REQUIRED");
        }

        app.setStatus(AdoptionApplicationStatus.APPROVED);
        app.setDecidedBy(decidedBy);
        app.setDecidedAt(LocalDateTime.now());
        app.setReadyAt(readyAt);
        app.setDecisionReasonCode(blankToNull(reasonCode));
        app.setUpdatedAt(LocalDateTime.now());
        AdoptionApplication saved = applicationRepository.save(app);
        notifyApplicant(saved, NotificationType.ADOPTION_APPROVED, "Đơn nhận nuôi " + code(saved) + " đã được duyệt",
                "Tổ chức sẽ liên hệ để hẹn ngày đón bé.");
        return saved;
    }

    /**
     * Reject an adoption application.
     * Validates the current status is PENDING before transitioning to REJECTED.
     */
    public AdoptionApplication reject(UUID applicationId, UUID decidedBy, String rejectReason) {
        return reject(applicationId, decidedBy, rejectReason, null);
    }

    public AdoptionApplication reject(UUID applicationId, UUID decidedBy, String rejectReason, String reasonCode) {
        log.info("Rejecting adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        accessControl.requireAdoptionDecider(app.getOrganizationId(), decidedBy);
        validateStatus(app, AdoptionApplicationStatus.PENDING, AdoptionApplicationStatus.REJECTED);

        if (rejectReason == null || rejectReason.isBlank()) {
            throw new BusinessException("rejectReason is required when rejecting adoption", "ADOPTION_REJECT_REASON_REQUIRED");
        }

        app.setStatus(AdoptionApplicationStatus.REJECTED);
        app.setDecidedBy(decidedBy);
        app.setDecidedAt(LocalDateTime.now());
        app.setRejectReason(rejectReason);
        app.setDecisionReasonCode(blankToNull(reasonCode));
        app.setUpdatedAt(LocalDateTime.now());
        AdoptionApplication saved = applicationRepository.save(app);
        notifyApplicant(saved, NotificationType.ADOPTION_REJECTED, "Đơn nhận nuôi " + code(saved) + " không được duyệt",
                "Lý do: " + rejectReason);
        return saved;
    }

    /**
     * Cancel an adoption application.
     * Validates the current status is PENDING before transitioning to CANCELED.
     */
    public AdoptionApplication cancel(UUID applicationId, UUID requesterId) {
        log.info("Canceling adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        accessControl.requireAdoptionCanceler(app.getApplicantId(), app.getOrganizationId(), requesterId);

        if (app.getStatus() != AdoptionApplicationStatus.PENDING
                && app.getStatus() != AdoptionApplicationStatus.APPROVED) {
            throw new BusinessException(
                    "Only PENDING or APPROVED applications can be canceled", "ADOPTION_INVALID_STATUS");
        }

        app.setStatus(AdoptionApplicationStatus.CANCELED);
        app.setUpdatedAt(LocalDateTime.now());

        AdoptionApplication saved = applicationRepository.save(app);
        if (requesterId.equals(saved.getApplicantId())) {
            notifications.notifyOrganizationHandlers(saved.getOrganizationId(), NotificationType.ADOPTION_CANCELED,
                    "Đơn nhận nuôi " + code(saved) + " đã bị huỷ", "Người nộp đã huỷ đơn.", data(saved));
        } else {
            notifyApplicant(saved, NotificationType.ADOPTION_CANCELED, "Đơn nhận nuôi " + code(saved) + " đã bị huỷ",
                    "Tổ chức đã huỷ đơn của bạn.");
        }
        return saved;
    }

    /**
     * Complete an approved adoption.
     * Transfers pet ownership from organization to adopter.
     * Changes pet status to ADOPTED.
     */
    public AdoptionApplication complete(UUID applicationId, UUID completedBy) {
        log.info("Completing adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        validateOrgOwner(app.getOrganizationId(), completedBy);
        validateStatus(app, AdoptionApplicationStatus.APPROVED, AdoptionApplicationStatus.COMPLETED);

        // Transfer ownership to the adopter
        petDomainService.transferOwnership(app.getPetId(), "USER", app.getApplicantId());
        
        // Update pet status to ADOPTED
        petDomainService.changeStatus(app.getPetId(), PetStatus.ADOPTED);
        adjustReputation(app.getApplicantId(), 50, "ADOPTION_COMPLETED", app.getApplicationId(), completedBy);
        
        // Update application status
        app.setStatus(AdoptionApplicationStatus.COMPLETED);
        app.setUpdatedAt(LocalDateTime.now());
        log.info("Adoption completed: pet {} transferred to user {}", app.getPetId(), app.getApplicantId());

        AdoptionApplication saved = applicationRepository.save(app);
        // Left open they could never succeed, and PENDING ones would use up the limit of those applicants.
        List<UUID> closedApplicants = applicationRepository.findOpenApplicantIdsForPet(app.getPetId(), app.getApplicationId());
        applicationRepository.cancelOpenForPet(app.getPetId(), app.getApplicationId(), AUTO_CANCEL_REASON, LocalDateTime.now());

        notifyApplicant(saved, NotificationType.ADOPTION_COMPLETED, "Nhận nuôi hoàn tất " + code(saved),
                "Bé đã chính thức là thú cưng của bạn.");
        notifications.notifyUsers(closedApplicants, NotificationType.ADOPTION_CANCELED,
                "Đơn nhận nuôi của bạn đã đóng", AUTO_CANCEL_REASON, Map.of("petId", String.valueOf(app.getPetId())));
        return saved;
    }

    public ReclaimLog reclaim(UUID applicationId, String reason, UUID proofId, UUID reclaimedBy) {
        log.info("Reclaiming adopted pet from application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        validateOrgOwner(app.getOrganizationId(), reclaimedBy);
        validateStatus(app, AdoptionApplicationStatus.COMPLETED, AdoptionApplicationStatus.COMPLETED);

        Pet pet = petDomainService.findById(app.getPetId());
        if (pet.getStatus() != PetStatus.ADOPTED) {
            throw new ConflictException("Pet must be ADOPTED before reclaim", "PET_NOT_ADOPTED");
        }

        petDomainService.transferOwnership(app.getPetId(), "ORGANIZATION", app.getOrganizationId());
        petDomainService.changeStatus(app.getPetId(), PetStatus.FOSTERING);
        adjustReputation(app.getApplicantId(), -300, "PET_RECLAIMED", app.getApplicationId(), reclaimedBy);

        ReclaimLog reclaimLog = ReclaimLog.builder()
                .reclaimId(UUID.randomUUID())
                .applicationId(app.getApplicationId())
                .petId(app.getPetId())
                .organizationId(app.getOrganizationId())
                .userId(app.getApplicantId())
                .reclaimedBy(reclaimedBy)
                .reason(reason)
                .proofId(proofId)
                .reputationDelta(-300)
                .reclaimedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        ReclaimLog savedLog = reclaimLogRepository.save(reclaimLog);
        notifyApplicant(app, NotificationType.ADOPTION_RECLAIMED, "Tổ chức đã thu hồi thú cưng (" + code(app) + ")",
                "Lý do: " + reason);
        return savedLog;
    }

    // ── Private helpers ─────────────────────────────

    private static final String AUTO_CANCEL_REASON = "Thú cưng đã được người khác nhận nuôi";

    private void notifyApplicant(AdoptionApplication app, NotificationType type, String title, String body) {
        notifications.notifyUsers(List.of(app.getApplicantId()), type, title, body, data(app));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String code(AdoptionApplication app) {
        return app.getAdoptionCode() == null ? "" : app.getAdoptionCode();
    }

    private static Map<String, String> data(AdoptionApplication app) {
        return Map.of("applicationId", String.valueOf(app.getApplicationId()), "petId", String.valueOf(app.getPetId()));
    }

    private void validateStatus(AdoptionApplication app, AdoptionApplicationStatus expectedCurrent, AdoptionApplicationStatus targetStatus) {
        if (app.getStatus() != expectedCurrent) {
            throw new BusinessException(
                    String.format("Cannot transition adoption application status from %s to %s",
                            app.getStatus(), targetStatus), "ADOPTION_INVALID_STATUS");
        }
    }

    private UUID resolvePetOrganizationOwner(UUID petId) {
        PetCurrentOwner currentOwner = currentOwnerRepository.findByPetId(petId)
                .orElseThrow(() -> new ConflictException("Pet has no organization owner", "PET_OWNER_NOT_FOUND"));
        if (!"ORGANIZATION".equals(currentOwner.getOwnerType())) {
            throw new ConflictException("Pet is not managed by an organization", "PET_NOT_ORG_MANAGED");
        }
        return currentOwner.getOwnerId();
    }

    private void validateOrgOwner(UUID organizationId, UUID userId) {
        if (!organizationDomainService.isOwner(organizationId, userId)) {
            throw new ForbiddenException("Only organization owner can perform this adoption action");
        }
    }

    private void adjustReputation(UUID userId, int delta, String reason, UUID referenceId, UUID actorId) {
        reputationDomainService.adjust(userId, delta, reason, "ADOPTION_APPLICATION", referenceId, actorId);
    }
}
