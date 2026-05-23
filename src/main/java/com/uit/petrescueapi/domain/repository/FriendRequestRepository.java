package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.FriendRequest;
import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository {

    FriendRequest save(FriendRequest request);

    Optional<FriendRequest> findById(UUID requestId);

    Optional<FriendRequest> findByRequesterAndAddressee(UUID requesterId, UUID addresseeId);

    Optional<FriendRequest> findPendingBetween(UUID requesterId, UUID addresseeId);

    Page<FriendRequest> findByAddresseeAndStatus(UUID addresseeId, FriendRequestStatus status, Pageable pageable);

    Page<FriendRequest> findByRequesterAndStatus(UUID requesterId, FriendRequestStatus status, Pageable pageable);
}
