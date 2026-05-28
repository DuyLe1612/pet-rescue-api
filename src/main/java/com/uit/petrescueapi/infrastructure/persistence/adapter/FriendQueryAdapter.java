package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import com.uit.petrescueapi.application.port.out.FriendQueryDataPort;
import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import com.uit.petrescueapi.infrastructure.persistence.repository.FriendRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendQueryAdapter implements FriendQueryDataPort {

    private final FriendRequestJpaRepository friendRequestRepository;

    @Override
    public Page<FriendSummaryDto> listFriends(UUID userId, String search, Pageable pageable) {
        return friendRequestRepository.findFriendsByUser(userId, search, pageable)
                .map(friend -> FriendSummaryDto.builder()
                        .userId(friend.getUserId())
                        .username(friend.getUsername())
                        .fullName(friend.getFullName())
                        .avatarUrl(friend.getAvatarUrl())
                        .build());
    }

    @Override
    public Page<FriendRequestDto> listPendingRequests(UUID userId, String search, Pageable pageable) {
        return friendRequestRepository.findByAddresseeAndStatus(userId, FriendRequestStatus.PENDING, search, pageable)
                .map(request -> FriendRequestDto.builder()
                        .id(request.getRequestId())
                        .requesterId(request.getRequesterId())
                        .addresseeId(request.getAddresseeId())
                        .status(request.getStatus().name())
                        .createdAt(request.getCreatedAt())
                        .build());
    }
}
