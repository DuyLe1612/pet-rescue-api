package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.EmailVerificationToken;
import com.uit.petrescueapi.domain.repository.EmailVerificationTokenRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.EmailVerificationTokenEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpa;
    private final EmailVerificationTokenEntityMapper mapper;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return jpa.findByToken(token).map(mapper::toDomain);
    }
}
