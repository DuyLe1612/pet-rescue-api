package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.port.command.FriendCommandPort;
import com.uit.petrescueapi.domain.entity.FriendRequest;
import com.uit.petrescueapi.domain.service.FriendDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendCommandUseCase implements FriendCommandPort {

    private final FriendDomainService friendDomainService;

    @Override
    public FriendRequestDto sendRequest(UUID requesterId, UUID addresseeId) {
        FriendRequest request = friendDomainService.sendRequest(requesterId, addresseeId);
        return toDto(request);
    }

    @Override
    public FriendRequestDto acceptRequest(UUID requestId, UUID userId) {
        FriendRequest request = friendDomainService.acceptRequest(requestId, userId);
        return toDto(request);
    }

    @Override
    public FriendRequestDto rejectRequest(UUID requestId, UUID userId) {
        FriendRequest request = friendDomainService.rejectRequest(requestId, userId);
        return toDto(request);
    }

    private FriendRequestDto toDto(FriendRequest request) {
        return FriendRequestDto.builder()
                .id(request.getId())
                .requesterId(request.getRequesterId())
                .addresseeId(request.getAddresseeId())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
