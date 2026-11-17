package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.event.MediaPromotionRequestedEvent;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.RescueCaseCommunityRepository;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import com.uit.petrescueapi.domain.valueobject.RescueCaseFlagReason;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * What people around a case add to it: progress notes from those working on it, and flags from
 * anyone who thinks it needs no more help. Neither changes the case itself; a closed case is only
 * closed by the people who handle it.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RescueCaseCommunityDomainService {

    /** Distinct people flagging an open case before the reporter and admins are told. */
    public static final int FLAG_ALERT_THRESHOLD = 3;

    private final RescueCaseDomainService caseDomainService;
    private final RescueClaimDomainService claimDomainService;
    private final AccessControlDomainService accessControl;
    private final RescueCaseCommunityRepository repository;
    private final NotificationDomainService notifications;
    private final ApplicationEventPublisher events;

    /**
     * The reporter, anyone with an active claim, the handling staff and admins may post. The media
     * must already be checked as the author's own uploads; the temporary ones move after commit.
     */
    public UUID postUpdate(UUID caseId, UUID authorId, String note, List<UUID> mediaIds, Collection<UUID> temporaryMediaIds) {
        RescueCase rescueCase = caseDomainService.findById(caseId);
        if (rescueCase.getStatus() == RescueCaseStatus.CLOSED) {
            throw new ConflictException("A closed case takes no more updates", "RESCUE_CASE_CLOSED");
        }
        List<UUID> claimers = activeClaimers(caseId);
        if (!claimers.contains(authorId)) {
            accessControl.requireCaseEditor(rescueCase, authorId);
        }

        UUID updateId = UUID.randomUUID();
        List<UUID> media = mediaIds == null ? List.of() : mediaIds.stream().filter(Objects::nonNull).distinct().toList();
        repository.saveUpdate(updateId, caseId, authorId, note.trim(), media);
        for (UUID mediaId : media) {
            if (temporaryMediaIds.contains(mediaId)) events.publishEvent(new MediaPromotionRequestedEvent(mediaId, "rescue-cases/" + caseId + "/updates"));
        }

        List<UUID> audience = new ArrayList<>(claimers);
        audience.add(rescueCase.getReportedBy());
        audience.remove(authorId);
        notifications.notifyUsers(audience, NotificationType.RESCUE_CASE_UPDATED,
                "Cập nhật mới ở ca " + rescueCase.getCaseCode(), preview(note), data(caseId));
        return updateId;
    }

    /** Anyone signed in except the reporter, on a case still waiting for help. Returns the flag count. */
    public int flag(UUID caseId, UUID userId, RescueCaseFlagReason reason, String note) {
        RescueCase rescueCase = caseDomainService.findById(caseId);
        if (userId.equals(rescueCase.getReportedBy())) {
            throw new ForbiddenException("Close your own case instead of flagging it");
        }
        if (rescueCase.getStatus() != RescueCaseStatus.REPORTED && rescueCase.getStatus() != RescueCaseStatus.IN_PROGRESS) {
            throw new ConflictException("Only an open case can be flagged", "RESCUE_CASE_NOT_OPEN");
        }
        repository.saveFlag(caseId, userId, reason, note == null || note.isBlank() ? null : note.trim());

        int count = repository.countFlags(caseId);
        if (count >= FLAG_ALERT_THRESHOLD && repository.markFlagAlerted(caseId)) {
            String title = "Ca " + rescueCase.getCaseCode() + " có thể không cần giúp nữa";
            notifications.notifyUsers(List.of(rescueCase.getReportedBy()), NotificationType.RESCUE_CASE_FLAGGED, title,
                    count + " người cho biết ca này sai hoặc đã được xử lý. Nếu đúng, hãy đóng ca.", data(caseId));
            notifications.notifyAdmins(NotificationType.RESCUE_CASE_FLAGGED, title,
                    count + " người gắn cờ ca này. Xem trong danh sách ca bị gắn cờ.", data(caseId));
        }
        return count;
    }

    /** Withdrawing a flag that does not exist is harmless. Returns the flag count. */
    public int unflag(UUID caseId, UUID userId) {
        caseDomainService.findById(caseId);
        repository.deleteFlag(caseId, userId);
        return repository.countFlags(caseId);
    }

    private List<UUID> activeClaimers(UUID caseId) {
        return claimDomainService.findActiveByCase(caseId).stream()
                .map(claim -> claim.getUserId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static String preview(String note) {
        String text = note.trim();
        return text.length() <= 140 ? text : text.substring(0, 137) + "...";
    }

    private static Map<String, String> data(UUID caseId) {
        return Map.of("caseId", caseId.toString());
    }
}
