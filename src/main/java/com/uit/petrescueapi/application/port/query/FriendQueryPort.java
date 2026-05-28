package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FriendQueryPort {

    Page<FriendSummaryDto> listFriends(UUID userId, String search, Pageable pageable);

    Page<FriendRequestDto> listPendingRequests(UUID userId, String search, Pageable pageable);
}
