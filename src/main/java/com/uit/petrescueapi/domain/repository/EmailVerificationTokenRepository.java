package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.EmailVerificationToken;

import java.util.Optional;

/**
 * Domain repository contract for email verification tokens.
 */
public interface EmailVerificationTokenRepository {

    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByToken(String token);
}
