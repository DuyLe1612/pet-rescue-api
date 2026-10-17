package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.PasswordResetToken;
import com.uit.petrescueapi.domain.repository.PasswordResetTokenRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PasswordResetTokenEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;
    private final PasswordResetTokenEntityMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpa.findByToken(token).map(mapper::toDomain);
    }
}
