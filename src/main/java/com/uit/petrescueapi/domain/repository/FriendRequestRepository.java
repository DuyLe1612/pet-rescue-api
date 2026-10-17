package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.FriendRequest;

import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository {

    FriendRequest save(FriendRequest request);

    Optional<FriendRequest> findById(UUID requestId);

    Optional<FriendRequest> findByRequesterAndAddressee(UUID requesterId, UUID addresseeId);

    Optional<FriendRequest> findPendingBetween(UUID requesterId, UUID addresseeId);
}
