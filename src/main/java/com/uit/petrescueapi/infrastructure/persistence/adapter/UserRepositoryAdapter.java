package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.repository.UserRepository;
import com.uit.petrescueapi.infrastructure.persistence.entity.UserJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.mapper.UserEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserEntityMapper mapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<String> findExpoPushTokenById(UUID id) {
        return jpa.findExpoPushTokenById(id);
    }

    @Override
    public java.util.Map<UUID, String> findExpoPushTokensByIds(java.util.Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }
        return jpa.findExpoPushTokensByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(UserJpaRepository.PushToken::getUserId, UserJpaRepository.PushToken::getToken));
    }

    @Override
    public boolean hasRole(UUID id, String roleCode) {
        return jpa.hasRole(id, roleCode);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }
}
