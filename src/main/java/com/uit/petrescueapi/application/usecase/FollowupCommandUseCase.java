package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.followup.FollowupAnswersRequestDto;
import com.uit.petrescueapi.application.dto.followup.FollowupAlertDto;
import com.uit.petrescueapi.application.dto.followup.FollowupRespondRequestDto;
import com.uit.petrescueapi.application.dto.followup.UpdateFollowupAlertRequestDto;
import com.uit.petrescueapi.application.port.command.FollowupCommandPort;
import com.uit.petrescueapi.domain.service.FollowupDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Thin: the transaction and the rules live in the domain service. */
@Service
@RequiredArgsConstructor
public class FollowupCommandUseCase implements FollowupCommandPort {

    private final FollowupDomainService followups;

    @Override
    public List<FollowupDomainService.Delivery> dispatchDue(int limit) {
        return followups.findDue(limit).stream().map(followups::dispatch).flatMap(Optional::stream).toList();
    }

    @Override
    public int remind() {
        return followups.remind();
    }

    @Override
    public int expire() {
        return followups.expire();
    }

    @Override
    public void respondWithToken(FollowupRespondRequestDto request) {
        followups.respondWithToken(request.token(), request.answers().toDomain());
    }

    @Override
    public FollowupAlertDto updateAlert(UUID alertId, UUID userId, UpdateFollowupAlertRequestDto request) {
        return FollowupQueryUseCase.toDto(followups.updateAlert(alertId, userId, request.status(), request.note(), request.resolution()));
    }

    @Override
    public void respond(UUID followupId, UUID userId, FollowupAnswersRequestDto answers) {
        followups.respond(followupId, userId, answers.toDomain());
    }
}
