package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import com.uit.petrescueapi.application.port.out.FriendQueryDataPort;
import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import com.uit.petrescueapi.infrastructure.persistence.projection.FriendRequestProjection;
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
        return friendRequestRepository.findFriendsByUserCursor(userId, null, search, pageable)
                .map(friend -> FriendSummaryDto.builder()
                        .userId(friend.getUserId())
                        .username(friend.getUsername())
                        .fullName(friend.getFullName())
                        .avatarUrl(friend.getAvatarUrl())
                .createdAt(friend.getCreatedAt())
                        .build());
    }

    @Override
    public Page<FriendRequestDto> listPendingRequests(UUID userId, String search, Pageable pageable) {
        return friendRequestRepository.findPendingByAddresseeCursor(userId, FriendRequestStatus.PENDING.name(), null, search, pageable)
            .map(this::toRequestDto);
    }

        @Override
        public Page<FriendSummaryDto> listFriendsByCursor(UUID userId, java.time.LocalDateTime cursor, String search, Pageable pageable) {
        return friendRequestRepository.findFriendsByUserCursor(userId, cursor, search, pageable)
            .map(friend -> FriendSummaryDto.builder()
                .userId(friend.getUserId())
                .username(friend.getUsername())
                .fullName(friend.getFullName())
                .avatarUrl(friend.getAvatarUrl())
                .createdAt(friend.getCreatedAt())
                .build());
        }

        @Override
        public Page<FriendRequestDto> listPendingRequestsByCursor(UUID userId, java.time.LocalDateTime cursor, String search, Pageable pageable) {
        return friendRequestRepository.findPendingByAddresseeCursor(userId, FriendRequestStatus.PENDING.name(), cursor, search, pageable)
            .map(this::toRequestDto);
        }

        private FriendRequestDto toRequestDto(FriendRequestProjection request) {
        return FriendRequestDto.builder()
            .id(request.getRequestId())
            .requesterId(request.getRequesterId())
            .requesterName(request.getRequesterName())
            .requesterAvatarUrl(request.getRequesterAvatarUrl())
            .addresseeId(request.getAddresseeId())
            .status(request.getStatus())
            .createdAt(request.getCreatedAt())
            .build();
        }
}
