package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.UserReputation;
import com.uit.petrescueapi.domain.repository.UserReputationRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.UserReputationEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.UserReputationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserReputationRepositoryAdapter implements UserReputationRepository {

    private final UserReputationJpaRepository jpa;
    private final UserReputationEntityMapper mapper;

    @Override
    public UserReputation save(UserReputation reputation) {
        return mapper.toDomain(jpa.save(mapper.toEntity(reputation)));
    }

    @Override
    public void addToScore(UUID userId, int delta, java.time.LocalDateTime now) {
        jpa.addToScore(userId, delta, now);
    }

    @Override
    public Optional<UserReputation> findByUserId(UUID userId) {
        // The @Id of UserReputationJpaEntity is userId, so findById works
        return jpa.findById(userId).map(mapper::toDomain);
    }
}
