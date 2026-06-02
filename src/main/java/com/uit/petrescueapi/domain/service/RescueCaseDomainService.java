package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.*;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<RescueCase> findAll(Pageable pageable) {
        return rescueCaseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<RescueCaseCompletion> findAllCompletions(Pageable pageable) {
        return rescueCaseCompletionRepository.findAll(pageable);
    }
    // ── Commands ────────────────────────────────────

    /**
     * Report a new rescue case.
     * Sets the id, status to REPORTED, and reportedAt timestamp.
     */
    public RescueCase report(RescueCase rescueCase) {
        log.info("Reporting new rescue case");
        rescueCase.setCaseId(UUID.randomUUID());
        rescueCase.setCaseCode(visualCodeRepository.nextRescueCaseCode());
        rescueCase.setStatus(RescueCaseStatus.REPORTED);
        rescueCase.setReportedAt(LocalDateTime.now());
        rescueCase.setCreatedAt(LocalDateTime.now());
        return rescueCaseRepository.save(rescueCase);
    }

    /**
     * Report
     */
    public RescueCaseCompletion reportCompletion(RescueCaseCompletion rescueCaseCompletion) {
        log.info("Reporting new rescue case completion");

        Optional<RescueCaseCompletion> rescueCaseCompletion1 = rescueCaseCompletionRepository.findByCaseId(rescueCaseCompletion.getCaseId());
        if  (rescueCaseCompletion1.isPresent()) {
            throw new BusinessException("RescueCaseCompletion already exists");
        }
        rescueCaseCompletion.setCompletionId(UUID.randomUUID());
        rescueCaseCompletion.setRescuedAt(LocalDateTime.now());
        rescueCaseCompletion.setCreatedAt(LocalDateTime.now());
        return rescueCaseCompletionRepository.Save(rescueCaseCompletion);
    }


    /**
     * Partial update of an existing rescue case.
     */
    public RescueCase update(UUID caseId, RescueCase patch) {
        log.debug("Updating rescue case {}", caseId);
        RescueCase existing = findById(caseId);
        applyUpdates(existing, patch);
        existing.setUpdatedAt(LocalDateTime.now());
        return rescueCaseRepository.save(existing);
    }

    /**
     * Change the status of a rescue case with transition validation.
     */
    public RescueCase changeStatus(UUID caseId, RescueCaseStatus newStatus) {
        log.info("Changing rescue case {} status to {}", caseId, newStatus);
        RescueCase rescueCase = findById(caseId);
        RescueCaseStatus current = rescueCase.getStatus();

        Set<RescueCaseStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition rescue case status from %s to %s", current, newStatus));
        }

        rescueCase.setStatus(newStatus);
        rescueCase.setUpdatedAt(LocalDateTime.now());

        if (newStatus == RescueCaseStatus.CLOSED || newStatus == RescueCaseStatus.RESCUED) {
            rescueCase.setResolvedAt(LocalDateTime.now());
        }

        return rescueCaseRepository.save(rescueCase);
    }
    public RescueCaseCompletion approveCompletion(UUID completionId, UUID userId) {
        log.debug("Approving rescue case completion {} by user {}", completionId,userId);

        Optional<RescueCaseCompletion> completion = findCompletionById(completionId);
        if (completion.get().getVerifiedAt() != null) {
            throw new BusinessException(
                    "Completion already approved"
            );
        }

        RescueCase rescueCase = findById(completion.get().getCaseId());

        if (rescueCase.getStatus()
                != RescueCaseStatus.REPORTED) {

            throw new BusinessException(
                    "Case is not awaiting approval"
            );
        }
        if  (completion.isPresent()) {
            rescueCase.setStatus(RescueCaseStatus.RESCUED);
            completion.get().setUpdatedAt(LocalDateTime.now());
            completion.get().setVerifiedAt(LocalDateTime.now());
            completion.get().setVerifiedBy(userId);
        }
        rescueCaseRepository.save(rescueCase);
        return  rescueCaseCompletionRepository.Save(completion.get());
    }

    // ── Private helpers ─────────────────────────────

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
        if (source.getPetId() != null)        target.setPetId(source.getPetId());
    }


}
