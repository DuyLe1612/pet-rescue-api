package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.followup.FollowupAnswersRequestDto;
import com.uit.petrescueapi.application.dto.followup.AdoptionOutcomeDto;
import com.uit.petrescueapi.application.dto.followup.FollowupAlertDto;
import com.uit.petrescueapi.application.dto.followup.RecordOutcomeRequestDto;
import com.uit.petrescueapi.application.dto.followup.ReturnRequestDto;
import com.uit.petrescueapi.application.dto.followup.SupportRequestDto;
import com.uit.petrescueapi.application.dto.followup.FollowupRespondRequestDto;
import com.uit.petrescueapi.application.dto.followup.UpdateFollowupAlertRequestDto;
import com.uit.petrescueapi.domain.service.FollowupDomainService;

import java.util.List;
import java.util.UUID;

/** Writes of the follow-up after an adoption. */
public interface FollowupCommandPort {

    /** Claims up to {@code limit} due milestones; the caller sends the emails, outside any transaction. */
    List<FollowupDomainService.Delivery> dispatchDue(int limit);

    int remind();

    int expire();

    void respondWithToken(FollowupRespondRequestDto request);

    void respond(UUID followupId, UUID userId, FollowupAnswersRequestDto answers);

    void requestSupport(UUID applicationId, UUID userId, SupportRequestDto request);

    void requestReturn(UUID applicationId, UUID userId, ReturnRequestDto request);

    void setPreference(UUID applicationId, UUID userId, boolean enabled);

    AdoptionOutcomeDto recordOutcome(UUID applicationId, UUID userId, RecordOutcomeRequestDto request);

    FollowupAlertDto updateAlert(UUID alertId, UUID userId, UpdateFollowupAlertRequestDto request);
}
