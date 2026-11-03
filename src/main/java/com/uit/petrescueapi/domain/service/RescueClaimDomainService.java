package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.event.RescueCaseClaimedEvent;
import com.uit.petrescueapi.domain.event.RescueCaseStatusChangedEvent;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.OrganizationMemberRepository;
import com.uit.petrescueapi.domain.repository.RescueCaseClaimRepository;
import com.uit.petrescueapi.domain.repository.RescueCaseCompletionRepository;
import com.uit.petrescueapi.domain.repository.RescueCaseRepository;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescueClaimReleaseReason;
import com.uit.petrescueapi.domain.valueobject.RescueClaimStatus;
import com.uit.petrescueapi.domain.valueobject.RescueCompletionStatus;
import com.uit.petrescueapi.domain.valueobject.RescueOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Who is working on a rescue case.
 *
 * <p>Personal claims do not lock a case and expire, so someone who never shows up cannot strand the
 * animal. An OWNER or STAFF claims on behalf of their organization, which does lock the case to it.</p>
 *
 * <p>Personal claims must not collide, so claim, release and expiry hold the case row until commit.
 * A case row also has a version, so a transaction must save a given case only once: a second save
 * from the same stale copy fails after the first flush.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RescueClaimDomainService {

    private static final Set<String> ORGANIZATION_CLAIM_ROLES = Set.of("OWNER", "STAFF");

    private final RescueCaseRepository cases;
    private final RescueCaseClaimRepository claims;
    private final RescueCaseCompletionRepository completions;
    private final OrganizationMemberRepository members;
    private final NotificationDomainService notifications;
    private final ApplicationEventPublisher events;

    @Value("${app.rescue.claim-hours:4}")
    private long claimHours = 4;

    @Value("${app.rescue.max-personal-claims:2}")
    private int maxPersonalClaims = 2;

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RescueCaseClaim> findActiveByCase(UUID caseId) {
        return claims.findActiveByCaseId(caseId);
    }

    @Transactional(readOnly = true)
    public List<RescueCaseClaim> findActiveByUser(UUID userId) {
        return claims.findActiveByUser(userId);
    }

    @Transactional(readOnly = true)
    public List<RescueCaseClaim> findExpired(LocalDateTime now, int limit) {
        return claims.findExpiredBefore(now, limit);
    }

    @Transactional(readOnly = true)
    public List<RescueCaseClaim> findToRemind(LocalDateTime before, int limit) {
        return claims.findToRemindBefore(before, limit);
    }

    // ── Commands ────────────────────────────────────

    public RescueCaseClaim claim(UUID caseId, UUID userId, boolean safetyAcknowledged) {
        RescueCase rescueCase = lockCase(caseId);
        Optional<UUID> organizationId = handlerOrganization(userId);

        if (organizationId.isPresent() && rescueCase.getStatus() == RescueCaseStatus.RESCUED) {
            return acceptHandover(rescueCase, organizationId.get(), userId);
        }
        requireOpen(rescueCase);

        Optional<RescueCaseClaim> existing = claims.findActiveByCaseAndUser(caseId, userId);
        if (existing.isPresent()) {
            return existing.get();          // double tap
        }

        RescueCaseClaim claim;
        if (organizationId.isPresent()) {
            Optional<RescueCaseClaim> organizationClaim = rescueCase.getOrganizationId() == null
                    ? Optional.empty()
                    : claims.findActiveByCaseAndOrganization(caseId, organizationId.get());
            if (organizationClaim.isPresent()) {
                return organizationClaim.get();
            }
            claim = takeForOrganization(rescueCase, organizationId.get(), userId);
        } else {
            if (!safetyAcknowledged) {
                throw new BusinessException("Please confirm the safety notice before claiming", "RESCUE_SAFETY_NOT_ACKNOWLEDGED");
            }
            if (claims.countActivePersonalByUser(userId) >= maxPersonalClaims) {
                throw new ConflictException("You can claim at most " + maxPersonalClaims + " cases at a time", "RESCUE_CLAIM_LIMIT");
            }
            claim = newClaim(caseId, userId, null, RescueClaimStatus.ACTIVE, LocalDateTime.now().plusHours(claimHours));
        }

        RescueCase fresh = findCase(caseId);
        boolean started = fresh.getStatus() == RescueCaseStatus.REPORTED;
        if (started) {
            fresh.setStatus(RescueCaseStatus.IN_PROGRESS);
        }
        if (organizationId.isPresent()) {
            fresh.setNeedsHelp(false);
        }
        fresh.setUpdatedAt(LocalDateTime.now());
        cases.save(fresh);
        if (started) {
            events.publishEvent(new RescueCaseStatusChangedEvent(caseId, RescueCaseStatus.IN_PROGRESS.name()));
        }

        if (!userId.equals(fresh.getReportedBy())) {
            notifications.notifyUsers(toList(fresh.getReportedBy()), NotificationType.RESCUE_CLAIMED,
                    "Có người nhận ca " + fresh.getCaseCode(),
                    organizationId.isPresent() ? "Một tổ chức đã tiếp nhận ca của bạn." : "Có người đang đến giúp ca của bạn.",
                    data(caseId));
        }
        if (organizationId.isPresent()) {
            List<UUID> helpers = claims.findActiveByCaseId(caseId).stream()
                    .filter(c -> c.getOrganizationId() == null)
                    .map(RescueCaseClaim::getUserId)
                    .toList();
            notifications.notifyUsers(helpers, NotificationType.RESCUE_ORGANIZATION_CLAIMED,
                    "Tổ chức đã tiếp nhận ca " + fresh.getCaseCode(),
                    "Bạn có thể trả ca, hoặc tiếp tục nếu đã tới nơi.", data(caseId));
        }
        events.publishEvent(new RescueCaseClaimedEvent(caseId, userId, fresh.getReportedBy()));
        log.info("User {} claimed rescue case {}", userId, caseId);
        return claim;
    }

    public RescueCaseClaim extend(UUID caseId, UUID userId) {
        RescueCaseClaim claim = claims.findActiveByCaseAndUser(caseId, userId)
                .orElseThrow(() -> new ConflictException("You have no active claim on this case", "RESCUE_CLAIM_NOT_FOUND"));
        if (claim.getExpiresAt() == null) {
            throw new ConflictException("An organization claim does not expire", "RESCUE_CLAIM_NOT_EXPIRING");
        }
        claim.setExpiresAt(LocalDateTime.now().plusHours(claimHours));
        claim.setRemindedAt(null);
        return claims.save(claim);
    }

    public RescueCaseClaim release(UUID caseId, UUID userId, RescueClaimReleaseReason reason) {
        RescueClaimReleaseReason effective = reason == null ? RescueClaimReleaseReason.GAVE_UP : reason;
        if (effective == RescueClaimReleaseReason.CASE_ENDED) {
            throw new BusinessException("Invalid release reason", "RESCUE_CLAIM_REASON_INVALID");
        }
        RescueCase rescueCase = lockCase(caseId);
        RescueCaseClaim claim = findOwnClaim(rescueCase, userId)
                .orElseThrow(() -> new ConflictException("You have no active claim on this case", "RESCUE_CLAIM_NOT_FOUND"));

        end(claim, RescueClaimStatus.RELEASED, effective);

        boolean changed = false;
        if (claim.getOrganizationId() != null && claim.getOrganizationId().equals(rescueCase.getOrganizationId())) {
            rescueCase.setOrganizationId(null);
            changed = true;
        }
        if (effective == RescueClaimReleaseReason.NEED_HELP) {
            rescueCase.setNeedsHelp(true);
            changed = true;
        }
        reopenIfIdle(rescueCase, changed);

        notifications.notifyUsers(toList(rescueCase.getReportedBy()), NotificationType.RESCUE_CLAIM_RELEASED,
                "Cập nhật ca " + rescueCase.getCaseCode(), releaseMessage(effective), data(caseId));
        return claim;
    }

    /** Runs one claim per transaction, so a bad row cannot hold back the rest. */
    public void expire(UUID claimId) {
        RescueCaseClaim claim = claims.findById(claimId).orElse(null);
        if (claim == null || claim.getStatus() != RescueClaimStatus.ACTIVE
                || claim.getExpiresAt() == null || claim.getExpiresAt().isAfter(LocalDateTime.now())) {
            return;     // extended or ended since it was selected
        }
        RescueCase rescueCase = lockCase(claim.getCaseId());
        end(claim, RescueClaimStatus.EXPIRED, null);
        reopenIfIdle(rescueCase, false);
        notifications.notifyUsers(toList(claim.getUserId(), rescueCase.getReportedBy()), NotificationType.RESCUE_CLAIM_EXPIRED,
                "Lượt nhận ca " + rescueCase.getCaseCode() + " đã hết hạn",
                "Ca được mở lại cho người khác nếu không còn ai đang nhận.", data(claim.getCaseId()));
    }

    public void remind(UUID claimId) {
        RescueCaseClaim claim = claims.findById(claimId).orElse(null);
        if (claim == null || claim.getStatus() != RescueClaimStatus.ACTIVE || claim.getRemindedAt() != null) {
            return;
        }
        claim.setRemindedAt(LocalDateTime.now());
        claims.save(claim);
        notifications.notifyUsers(List.of(claim.getUserId()), NotificationType.RESCUE_CLAIM_EXPIRING,
                "Lượt nhận ca sắp hết hạn", "Gia hạn nếu bạn vẫn đang xử lý ca này.", data(claim.getCaseId()));
    }

    // ── Used by the completion workflow ─────────────

    /**
     * Whoever really rescued the animal must not be blocked for not pressing claim first, so sending
     * a report creates the claim when missing, without the personal claim limit. The claim ends here:
     * the pending report is what keeps the case IN_PROGRESS from now on.
     */
    RescueCaseClaim settleClaimForCompletion(RescueCase rescueCase, UUID userId) {
        UUID caseId = rescueCase.getCaseId();
        Optional<UUID> organizationId = handlerOrganization(userId)
                .filter(id -> rescueCase.getOrganizationId() == null || id.equals(rescueCase.getOrganizationId()));

        if (organizationId.isPresent()) {
            if (rescueCase.getOrganizationId() == null && !cases.assignOrganization(caseId, organizationId.get())) {
                throw new ConflictException("Another organization has taken this case", "RESCUE_CASE_TAKEN");
            }
            Optional<RescueCaseClaim> held = claims.findActiveByCaseAndOrganization(caseId, organizationId.get());
            if (held.isPresent()) {
                end(held.get(), RescueClaimStatus.COMPLETED, null);
                return held.get();
            }
            return newClaim(caseId, userId, organizationId.get(), RescueClaimStatus.COMPLETED, null);
        }

        Optional<RescueCaseClaim> own = claims.findActiveByCaseAndUser(caseId, userId);
        if (own.isPresent()) {
            end(own.get(), RescueClaimStatus.COMPLETED, null);
            return own.get();
        }
        return newClaim(caseId, userId, null, RescueClaimStatus.COMPLETED, null);
    }

    /** The case ended: everyone still on the way is told to stop. Does not touch the case row. */
    void endAllActive(RescueCase rescueCase, UUID exceptUserId) {
        List<RescueCaseClaim> active = claims.findActiveByCaseId(rescueCase.getCaseId());
        active.forEach(claim -> end(claim, RescueClaimStatus.RELEASED, RescueClaimReleaseReason.CASE_ENDED));
        notifications.notifyUsers(
                active.stream().map(RescueCaseClaim::getUserId).filter(id -> !id.equals(exceptUserId)).toList(),
                NotificationType.RESCUE_CASE_ENDED,
                "Ca " + rescueCase.getCaseCode() + " đã kết thúc", "Bạn không cần đến nữa. Cảm ơn bạn.",
                data(rescueCase.getCaseId()));
    }

    /** Back to REPORTED when nobody is working on the case any more. */
    void reopenIfIdle(RescueCase rescueCase, boolean alreadyChanged) {
        boolean idle = rescueCase.getStatus() == RescueCaseStatus.IN_PROGRESS
                && claims.countActiveByCaseId(rescueCase.getCaseId()) == 0
                && completions.findOpenByCaseId(rescueCase.getCaseId()).isEmpty();
        if (idle) {
            rescueCase.setStatus(RescueCaseStatus.REPORTED);
        }
        if (idle || alreadyChanged) {
            rescueCase.setUpdatedAt(LocalDateTime.now());
            cases.save(rescueCase);
        }
        if (idle) {
            events.publishEvent(new RescueCaseStatusChangedEvent(rescueCase.getCaseId(), RescueCaseStatus.REPORTED.name()));
        }
    }

    // ── Private helpers ─────────────────────────────

    private RescueCaseClaim takeForOrganization(RescueCase rescueCase, UUID organizationId, UUID userId) {
        UUID caseId = rescueCase.getCaseId();
        boolean alreadyOurs = organizationId.equals(rescueCase.getOrganizationId());
        if (!alreadyOurs && !cases.assignOrganization(caseId, organizationId)) {
            throw new ConflictException("Another organization has taken this case", "RESCUE_CASE_TAKEN");
        }
        return newClaim(caseId, userId, organizationId, RescueClaimStatus.ACTIVE, null);
    }

    /** An organization takes over an animal someone already rescued and wants to hand over. */
    private RescueCaseClaim acceptHandover(RescueCase rescueCase, UUID organizationId, UUID userId) {
        UUID caseId = rescueCase.getCaseId();
        if (organizationId.equals(rescueCase.getOrganizationId())) {
            throw new ConflictException("Your organization already holds this case", "RESCUE_CASE_ALREADY_HELD");
        }
        var completion = completions.findOpenByCaseId(caseId)
                .filter(c -> c.getStatus() == RescueCompletionStatus.APPROVED)
                .filter(c -> c.getOutcome() == RescueOutcome.HANDED_TO_ORG || c.getOutcome() == RescueOutcome.TEMP_HOLDING)
                .orElseThrow(() -> new ConflictException("This case is not waiting for an organization", "RESCUE_CASE_NOT_OPEN"));
        if (rescueCase.getPetId() != null || !cases.assignOrganization(caseId, organizationId)) {
            throw new ConflictException("Another organization has taken this case", "RESCUE_CASE_TAKEN");
        }
        RescueCaseClaim claim = newClaim(caseId, userId, organizationId, RescueClaimStatus.COMPLETED, null);
        notifications.notifyUsers(toList(rescueCase.getReportedBy(), completion.getSubmittedBy()),
                NotificationType.RESCUE_ORGANIZATION_CLAIMED, "Tổ chức đã tiếp nhận ca " + rescueCase.getCaseCode(),
                "Tổ chức sẽ liên hệ để nhận bàn giao.", data(caseId));
        return claim;
    }

    private Optional<RescueCaseClaim> findOwnClaim(RescueCase rescueCase, UUID userId) {
        Optional<RescueCaseClaim> own = claims.findActiveByCaseAndUser(rescueCase.getCaseId(), userId);
        if (own.isPresent()) {
            return own;
        }
        return handlerOrganization(userId).flatMap(id -> claims.findActiveByCaseAndOrganization(rescueCase.getCaseId(), id));
    }

    private Optional<UUID> handlerOrganization(UUID userId) {
        String role = members.findOrgRoleByUserId(userId);
        if (role == null || !ORGANIZATION_CLAIM_ROLES.contains(role)) {
            return Optional.empty();
        }
        return Optional.ofNullable(members.findOrganizationIdByUserId(userId));
    }

    private RescueCaseClaim newClaim(UUID caseId, UUID userId, UUID organizationId, RescueClaimStatus status,
                                     LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return claims.save(RescueCaseClaim.builder()
                .claimId(UUID.randomUUID())
                .caseId(caseId)
                .userId(userId)
                .organizationId(organizationId)
                .status(status)
                .claimedAt(now)
                .expiresAt(expiresAt)
                .endedAt(status == RescueClaimStatus.ACTIVE ? null : now)
                .build());
    }

    private void end(RescueCaseClaim claim, RescueClaimStatus status, RescueClaimReleaseReason reason) {
        claim.setStatus(status);
        claim.setReleaseReason(reason);
        claim.setEndedAt(LocalDateTime.now());
        claims.save(claim);
    }

    private RescueCase lockCase(UUID caseId) {
        return cases.findByIdForUpdate(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", "caseId", caseId));
    }

    private RescueCase findCase(UUID caseId) {
        return cases.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", "caseId", caseId));
    }

    private static void requireOpen(RescueCase rescueCase) {
        if (rescueCase.getStatus() != RescueCaseStatus.REPORTED && rescueCase.getStatus() != RescueCaseStatus.IN_PROGRESS) {
            throw new ConflictException("This case has already ended", "RESCUE_CASE_NOT_OPEN");
        }
    }

    private static String releaseMessage(RescueClaimReleaseReason reason) {
        return switch (reason) {
            case NOT_FOUND -> "Người nhận ca đã tới nơi nhưng không tìm thấy bé. Ca vẫn mở.";
            case NEED_HELP -> "Người nhận ca cần hỗ trợ thêm. Ca được đánh dấu cần hỗ trợ.";
            default -> "Người nhận ca đã trả ca.";
        };
    }

    private static Map<String, String> data(UUID caseId) {
        return Map.of("caseId", caseId.toString());
    }

    /** {@code List.of} rejects null, and old cases have no reporter. */
    private static List<UUID> toList(UUID... ids) {
        return java.util.Arrays.stream(ids).filter(java.util.Objects::nonNull).toList();
    }
}
