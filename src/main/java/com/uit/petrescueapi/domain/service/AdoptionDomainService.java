package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.ReclaimLog;
import com.uit.petrescueapi.domain.entity.UserReputation;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.exception.UnprocessableEntityException;
import com.uit.petrescueapi.domain.repository.AdoptionApplicationRepository;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.repository.ReclaimLogRepository;
import com.uit.petrescueapi.domain.repository.UserReputationRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
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
    private final PetCurrentOwnerRepository currentOwnerRepository;
    private final UserReputationRepository userReputationRepository;
    private final ReclaimLogRepository reclaimLogRepository;
    private final JdbcTemplate jdbcTemplate;

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

        if (application.getOrganizationId() == null) {
            application.setOrganizationId(resolvePetOrganizationOwner(application.getPetId()));
        }

        application.setApplicationId(UUID.randomUUID());
        application.setAdoptionCode(visualCodeRepository.nextAdoptionCode());
        application.setStatus(AdoptionApplicationStatus.PENDING);
        application.setCreatedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    /**
     * Approve an adoption application.
     * Validates the current status is PENDING before transitioning to APPROVED.
     */
    public AdoptionApplication approve(UUID applicationId, UUID decidedBy, LocalDateTime readyAt) {
        log.info("Approving adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        validateStatus(app, AdoptionApplicationStatus.PENDING, AdoptionApplicationStatus.APPROVED);

        if (readyAt == null) {
            throw new IllegalArgumentException("readyAt is required when approving adoption");
        }

        app.setStatus(AdoptionApplicationStatus.APPROVED);
        app.setDecidedBy(decidedBy);
        app.setDecidedAt(LocalDateTime.now());
        app.setReadyAt(readyAt);
        app.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(app);
    }

    /**
     * Reject an adoption application.
     * Validates the current status is PENDING before transitioning to REJECTED.
     */
    public AdoptionApplication reject(UUID applicationId, UUID decidedBy, String rejectReason) {
        log.info("Rejecting adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);
        validateStatus(app, AdoptionApplicationStatus.PENDING, AdoptionApplicationStatus.REJECTED);

        if (rejectReason == null || rejectReason.isBlank()) {
            throw new IllegalArgumentException("rejectReason is required when rejecting adoption");
        }

        app.setStatus(AdoptionApplicationStatus.REJECTED);
        app.setDecidedBy(decidedBy);
        app.setDecidedAt(LocalDateTime.now());
        app.setRejectReason(rejectReason);
        app.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(app);
    }

    /**
     * Cancel an adoption application.
     * Validates the current status is PENDING before transitioning to CANCELED.
     */
    public AdoptionApplication cancel(UUID applicationId) {
        log.info("Canceling adoption application {}", applicationId);
        AdoptionApplication app = findById(applicationId);

        if (app.getStatus() != AdoptionApplicationStatus.PENDING
                && app.getStatus() != AdoptionApplicationStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only PENDING or APPROVED applications can be canceled");
        }

        app.setStatus(AdoptionApplicationStatus.CANCELED);
        app.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(app);
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
        
        return applicationRepository.save(app);
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
        return reclaimLogRepository.save(reclaimLog);
    }

    // ── Private helpers ─────────────────────────────

    private void validateStatus(AdoptionApplication app, AdoptionApplicationStatus expectedCurrent, AdoptionApplicationStatus targetStatus) {
        if (app.getStatus() != expectedCurrent) {
            throw new IllegalStateException(
                    String.format("Cannot transition adoption application status from %s to %s",
                            app.getStatus(), targetStatus));
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
        UserReputation reputation = userReputationRepository.findByUserId(userId)
                .orElse(UserReputation.builder()
                        .userId(userId)
                        .score(0)
                        .level("NEW")
                        .build());
        reputation.setScore((reputation.getScore() == null ? 0 : reputation.getScore()) + delta);
        reputation.setUpdatedAt(LocalDateTime.now());
        userReputationRepository.save(reputation);
        jdbcTemplate.update("""
                INSERT INTO reputation_logs (log_id, user_id, delta, reason, reference_type, reference_id, created_at, created_by)
                VALUES (?, ?, ?, ?, 'ADOPTION_APPLICATION', ?, ?, ?)
                """,
                UUID.randomUUID(),
                userId,
                delta,
                reason,
                referenceId,
                LocalDateTime.now(),
                actorId);
    }
}
