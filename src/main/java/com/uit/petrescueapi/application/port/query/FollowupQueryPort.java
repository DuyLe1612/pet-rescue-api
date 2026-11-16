package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.followup.FollowupAlertDto;
import com.uit.petrescueapi.application.dto.followup.FollowupDto;
import com.uit.petrescueapi.application.dto.followup.FollowupFormDto;

import java.util.List;
import java.util.UUID;

public interface FollowupQueryPort {

    FollowupFormDto formFor(String token);

    List<FollowupDto> timeline(UUID applicationId, UUID viewerId);

    List<FollowupAlertDto> alerts(UUID userId, String status);
}
