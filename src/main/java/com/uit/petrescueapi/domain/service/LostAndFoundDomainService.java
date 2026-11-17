package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.exception.ConflictException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.CaseMatchRepository;
import com.uit.petrescueapi.domain.valueobject.CaseMatchCandidate;
import com.uit.petrescueapi.domain.valueobject.CaseMatchStatus;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.domain.valueobject.RescueCaseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lost pets and found animals. Suggestions come from {@link CaseMatchRules}; only the owner says
 * whether a suggestion is their pet, and nothing closes a case without them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LostAndFoundDomainService {

    private final RescueCaseDomainService caseDomainService;
    private final PetDomainService petDomainService;
    private final AccessControlDomainService accessControl;
    private final CaseMatchRepository matches;
    private final NotificationDomainService notifications;

    /** An owner reports their own pet lost. The pet becomes LOST and remembers what it was. */
    public RescueCase reportLost(RescueCase draft, UUID petId, UUID ownerId) {
        accessControl.requirePetManager(petId, ownerId);
        if (matches.findOpenLostCaseOfPet(petId).isPresent()) {
            throw new ConflictException("This pet already has an open lost report", "PET_ALREADY_LOST");
        }
        Pet pet = petDomainService.findById(petId);
        draft.setPetId(petId);
        draft.setCaseType(RescueCaseType.LOST);
        draft.setReportedBy(ownerId);
        draft.setPetStatusBeforeLost(pet.getStatus() == null ? null : pet.getStatus().name());
        if (draft.getColor() == null) {
            draft.setColor(pet.getColor());
        }
        RescueCase created = caseDomainService.report(draft);
        if (pet.getStatus() != null && pet.getStatus() != PetStatus.LOST) {
            petDomainService.changeStatus(petId, PetStatus.LOST);
        }
        return created;
    }

    /**
     * Compares a newly reported or converted case with the other side, and tells each lost pet's owner.
     * Runs after the report committed, so it needs a transaction of its own.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public int suggestFor(UUID caseId) {
        List<CaseMatchCandidate> candidates = matches.findCandidates(caseId);
        Map<UUID, Integer> newPerOwner = new HashMap<>();
        Map<UUID, UUID> lostCaseOfOwner = new HashMap<>();
        for (CaseMatchCandidate candidate : candidates) {
            CaseMatchRules.Score score = CaseMatchRules.score(candidate);
            boolean created = matches.saveSuggestion(UUID.randomUUID(), candidate.lostCaseId(), candidate.otherCaseId(),
                    (int) Math.round(candidate.distanceMeters()), score.score(), score.reasons());
            if (created && candidate.lostReporterId() != null) {
                newPerOwner.merge(candidate.lostReporterId(), 1, Integer::sum);
                lostCaseOfOwner.putIfAbsent(candidate.lostReporterId(), candidate.lostCaseId());
            }
        }
        newPerOwner.forEach((owner, count) -> notifications.notifyUsers(List.of(owner), NotificationType.RESCUE_MATCH_SUGGESTED,
                count == 1 ? "Có một tin có thể là bé của bạn" : "Có " + count + " tin có thể là bé của bạn",
                "Mở tin báo lạc để xem và xác nhận.", Map.of("caseId", lostCaseOfOwner.get(owner).toString())));
        return candidates.size();
    }

    /**
     * The owner of the lost case says the other one is their pet. Both lost and found cases close as
     * REUNITED and point at each other; a rescue case only records the link and its handlers are told.
     */
    public RescueCase confirm(UUID lostCaseId, UUID suggestionId, UUID userId) {
        CaseMatchRepository.Suggestion suggestion = pendingSuggestionOf(lostCaseId, suggestionId);
        RescueCase lost = caseDomainService.findById(lostCaseId);
        requireOwner(lost, userId);

        matches.decide(suggestionId, CaseMatchStatus.CONFIRMED, userId);
        matches.dismissPending(lostCaseId, suggestionId);
        RescueCase other = caseDomainService.recordReunion(suggestion.otherCaseId(), lostCaseId);
        RescueCase closed = caseDomainService.recordReunion(lostCaseId, suggestion.otherCaseId());
        restorePet(lost);

        Map<String, String> data = Map.of("caseId", other.getCaseId().toString());
        if (other.typeOrRescue() == RescueCaseType.RESCUE) {
            notifications.notifyUsers(List.of(other.getReportedBy()), NotificationType.RESCUE_MATCH_CONFIRMED,
                    "Ca " + other.getCaseCode() + " có thể là thú đi lạc",
                    "Chủ của bé đã nhận ra qua tin báo lạc " + lost.getCaseCode() + ".", data);
            if (other.getOrganizationId() != null) {
                notifications.notifyOrganizationHandlers(other.getOrganizationId(), NotificationType.RESCUE_MATCH_CONFIRMED,
                        "Ca " + other.getCaseCode() + " có chủ", "Chủ đã nhận ra bé qua tin báo lạc " + lost.getCaseCode() + ".", data);
            }
        } else {
            notifications.notifyUsers(List.of(other.getReportedBy()), NotificationType.RESCUE_MATCH_CONFIRMED,
                    "Chủ đã nhận ra bé ở tin " + other.getCaseCode(), "Cảm ơn bạn đã báo. Tin đã được đóng.", data);
        }
        return closed;
    }

    public void reject(UUID lostCaseId, UUID suggestionId, UUID userId) {
        pendingSuggestionOf(lostCaseId, suggestionId);
        requireOwner(caseDomainService.findById(lostCaseId), userId);
        matches.decide(suggestionId, CaseMatchStatus.REJECTED, userId);
    }

    /** A lost case closed another way: its pet stops being lost and its suggestions are dropped. */
    public void onLostCaseClosed(UUID lostCaseId) {
        RescueCase lost = caseDomainService.findById(lostCaseId);
        if (lost.typeOrRescue() != RescueCaseType.LOST) {
            return;
        }
        matches.dismissPending(lostCaseId, null);
        restorePet(lost);
    }

    private CaseMatchRepository.Suggestion pendingSuggestionOf(UUID lostCaseId, UUID suggestionId) {
        CaseMatchRepository.Suggestion suggestion = matches.findSuggestion(suggestionId)
                .filter(s -> s.lostCaseId().equals(lostCaseId))
                .orElseThrow(() -> new ResourceNotFoundException("CaseMatchSuggestion", "suggestionId", suggestionId));
        if (suggestion.status() != CaseMatchStatus.PENDING) {
            throw new ConflictException("This suggestion was already decided", "MATCH_ALREADY_DECIDED");
        }
        return suggestion;
    }

    /** Only the owner knows their pet; an admin may act for them. */
    private void requireOwner(RescueCase lost, UUID userId) {
        if (!userId.equals(lost.getReportedBy()) && !accessControl.isAdmin(userId)) {
            throw new ForbiddenException("Only the owner who reported the pet lost can decide");
        }
    }

    private void restorePet(RescueCase lost) {
        if (lost.getPetId() == null || lost.getPetStatusBeforeLost() == null) {
            return;
        }
        Pet pet = petDomainService.findById(lost.getPetId());
        if (pet.getStatus() == PetStatus.LOST) {
            petDomainService.changeStatus(pet.getId(), PetStatus.valueOf(lost.getPetStatusBeforeLost()));
        }
    }
}
