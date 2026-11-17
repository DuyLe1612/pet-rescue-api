package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.valueobject.CaseMatchCandidate;
import com.uit.petrescueapi.domain.valueobject.CaseMatchStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseMatchRepository {

    /** Pairs for a case of either side, filtered by place, time window and species. */
    List<CaseMatchCandidate> findCandidates(UUID caseId);

    /** False when the pair was already suggested. */
    boolean saveSuggestion(UUID suggestionId, UUID lostCaseId, UUID otherCaseId, int distanceMeters, int score, List<String> reasons);

    Optional<Suggestion> findSuggestion(UUID suggestionId);

    void decide(UUID suggestionId, CaseMatchStatus status, UUID decidedBy);

    /** Every still pending suggestion of a lost case, except one. */
    void dismissPending(UUID lostCaseId, UUID except);

    Optional<UUID> findOpenLostCaseOfPet(UUID petId);

    record Suggestion(UUID suggestionId, UUID lostCaseId, UUID otherCaseId, CaseMatchStatus status) {
    }
}
