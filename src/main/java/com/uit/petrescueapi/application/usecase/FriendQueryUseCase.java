package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import com.uit.petrescueapi.application.port.out.FriendQueryDataPort;
import com.uit.petrescueapi.application.port.query.FriendQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendQueryUseCase implements FriendQueryPort {

    private final FriendQueryDataPort friendQueryDataPort;

    @Override
    public Page<FriendSummaryDto> listFriends(UUID userId, Pageable pageable) {
        log.debug("Query: list friends for user {}", userId);
        return friendQueryDataPort.listFriends(userId, pageable);
    }

    @Override
    public Page<FriendRequestDto> listPendingRequests(UUID userId, Pageable pageable) {
        log.debug("Query: list pending requests for user {}", userId);
        return friendQueryDataPort.listPendingRequests(userId, pageable);
    }
}
