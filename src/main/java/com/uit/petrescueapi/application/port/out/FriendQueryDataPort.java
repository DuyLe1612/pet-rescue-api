package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FriendQueryDataPort {

    Page<FriendSummaryDto> listFriends(UUID userId, Pageable pageable);

    Page<FriendRequestDto> listPendingRequests(UUID userId, Pageable pageable);
}
