package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.valueobject.RescueCaseFlagReason;

import java.util.List;
import java.util.UUID;

/** Progress notes and community flags of rescue cases. */
public interface RescueCaseCommunityRepository {

    void saveUpdate(UUID updateId, UUID caseId, UUID authorId, String note, List<UUID> mediaIds);

    /** One flag per person and case: flagging again replaces the reason and note. */
    void saveFlag(UUID caseId, UUID userId, RescueCaseFlagReason reason, String note);

    void deleteFlag(UUID caseId, UUID userId);

    int countFlags(UUID caseId);

    /** True only for the call that first marks the case, so the alert goes out once. */
    boolean markFlagAlerted(UUID caseId);
}
