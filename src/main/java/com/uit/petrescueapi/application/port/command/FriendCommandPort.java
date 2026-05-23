package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;

import java.util.UUID;

public interface FriendCommandPort {

    FriendRequestDto sendRequest(UUID requesterId, UUID addresseeId);

    FriendRequestDto acceptRequest(UUID requestId, UUID userId);

    FriendRequestDto rejectRequest(UUID requestId, UUID userId);
}
