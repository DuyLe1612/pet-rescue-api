package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import com.uit.petrescueapi.application.dto.comment.CursorPageDto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FriendQueryPort {

    CursorPageDto<FriendSummaryDto> listFriendsByCursor(UUID userId, LocalDateTime cursor, int size, String search);

    CursorPageDto<FriendRequestDto> listPendingRequestsByCursor(UUID userId, LocalDateTime cursor, int size, String search);
}
