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
    public Page<FriendSummaryDto> listFriends(UUID userId, String search, Pageable pageable) {
        log.debug("Query: list friends for user {} (search={})", userId, search);
        return friendQueryDataPort.listFriends(userId, search, pageable);
    }

    @Override
    public Page<FriendRequestDto> listPendingRequests(UUID userId, String search, Pageable pageable) {
        log.debug("Query: list pending requests for user {} (search={})", userId, search);
        return friendQueryDataPort.listPendingRequests(userId, search, pageable);
    }
}
