package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.AdoptionApplicationRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        application.setApplicationId(UUID.randomUUID());
        application.setAdoptionCode(visualCodeRepository.nextAdoptionCode());
        application.setStatus("PENDING");
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
        validateStatus(app, "PENDING", "APPROVED");

        if (readyAt == null) {
            throw new IllegalArgumentException("readyAt is required when approving adoption");
        }

        app.setStatus("APPROVED");
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
        validateStatus(app, "PENDING", "REJECTED");

        if (rejectReason == null || rejectReason.isBlank()) {
            throw new IllegalArgumentException("rejectReason is required when rejecting adoption");
        }

        app.setStatus("REJECTED");
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

        if (!"PENDING".equals(app.getStatus())
                && !"APPROVED".equals(app.getStatus())) {
            throw new IllegalStateException(
                    "Only PENDING or APPROVED applications can be canceled");
        }

        app.setStatus("CANCELED");
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
        validateStatus(app, "APPROVED", "COMPLETED");

        // Transfer ownership to the adopter
        petDomainService.transferOwnership(app.getPetId(), "USER", app.getApplicantId());
        
        // Update pet status to ADOPTED
        petDomainService.changeStatus(app.getPetId(), PetStatus.ADOPTED);
        
        // Update application status
        app.setStatus("COMPLETED");
        app.setUpdatedAt(LocalDateTime.now());
        log.info("Adoption completed: pet {} transferred to user {}", app.getPetId(), app.getApplicantId());
        
        return applicationRepository.save(app);
    }

    // ── Private helpers ─────────────────────────────

    private void validateStatus(AdoptionApplication app, String expectedCurrent, String targetStatus) {
        if (!expectedCurrent.equals(app.getStatus())) {
            throw new IllegalStateException(
                    String.format("Cannot transition adoption application status from %s to %s",
                            app.getStatus(), targetStatus));
        }
    }
}
