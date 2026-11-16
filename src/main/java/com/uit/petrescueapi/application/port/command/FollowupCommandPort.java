package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.followup.FollowupAnswersRequestDto;
import com.uit.petrescueapi.application.dto.followup.FollowupRespondRequestDto;
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
}
