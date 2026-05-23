package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.FriendRequest;
import com.uit.petrescueapi.domain.repository.FriendRequestRepository;
import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import com.uit.petrescueapi.infrastructure.persistence.mapper.ChatEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.FriendRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FriendRequestRepositoryAdapter implements FriendRequestRepository {

    private final FriendRequestJpaRepository jpaRepository;
    private final ChatEntityMapper mapper;

    @Override
    public FriendRequest save(FriendRequest request) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(request)));
    }

    @Override
    public Optional<FriendRequest> findById(UUID requestId) {
        return jpaRepository.findById(requestId).map(mapper::toDomain);
    }

    @Override
    public Optional<FriendRequest> findByRequesterAndAddressee(UUID requesterId, UUID addresseeId) {
        return jpaRepository.findByRequesterIdAndAddresseeId(requesterId, addresseeId).map(mapper::toDomain);
    }

    @Override
    public Optional<FriendRequest> findPendingBetween(UUID requesterId, UUID addresseeId) {
        return jpaRepository.findByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING)
                .map(mapper::toDomain);
    }

    @Override
    public Page<FriendRequest> findByAddresseeAndStatus(UUID addresseeId, FriendRequestStatus status, Pageable pageable) {
        return jpaRepository.findByAddresseeAndStatus(addresseeId, status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<FriendRequest> findByRequesterAndStatus(UUID requesterId, FriendRequestStatus status, Pageable pageable) {
        return jpaRepository.findByRequesterAndStatus(requesterId, status, pageable).map(mapper::toDomain);
    }
}
