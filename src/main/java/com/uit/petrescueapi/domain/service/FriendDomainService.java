package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.FriendRequest;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.FriendRequestRepository;
import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FriendDomainService {

    private final FriendRequestRepository friendRequestRepository;

    public FriendRequest sendRequest(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new BusinessException("Cannot send friend request to yourself", "FRIEND_SELF");
        }

        friendRequestRepository.findPendingBetween(requesterId, addresseeId)
                .ifPresent(existing -> {
                    throw new BusinessException("Friend request already exists", "FRIEND_EXISTS");
                });

        friendRequestRepository.findPendingBetween(addresseeId, requesterId)
                .ifPresent(existing -> {
                    throw new BusinessException("Reverse friend request already exists", "FRIEND_EXISTS");
                });

        LocalDateTime now = LocalDateTime.now();
        FriendRequest existing = friendRequestRepository.findByRequesterAndAddressee(requesterId, addresseeId).orElse(null);
        if (existing != null) {
            existing.setStatus(FriendRequestStatus.PENDING);
            existing.setRespondedAt(null);
            existing.setUpdatedAt(now);
            return friendRequestRepository.save(existing);
        }

        FriendRequest request = FriendRequest.builder()
            .id(UUID.randomUUID())
            .requesterId(requesterId)
            .addresseeId(addresseeId)
            .status(FriendRequestStatus.PENDING)
            .createdAt(now)
            .build();

        return friendRequestRepository.save(request);
    }

    public FriendRequest acceptRequest(UUID requestId, UUID userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("FriendRequest", "id", requestId));

        if (!request.getAddresseeId().equals(userId)) {
            throw new BusinessException("Not allowed to accept this request", "FRIEND_FORBIDDEN");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException("Friend request is not pending", "FRIEND_NOT_PENDING");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());

        return friendRequestRepository.save(request);
    }

    public FriendRequest rejectRequest(UUID requestId, UUID userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("FriendRequest", "id", requestId));

        if (!request.getAddresseeId().equals(userId)) {
            throw new BusinessException("Not allowed to reject this request", "FRIEND_FORBIDDEN");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException("Friend request is not pending", "FRIEND_NOT_PENDING");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());

        return friendRequestRepository.save(request);
    }
}
