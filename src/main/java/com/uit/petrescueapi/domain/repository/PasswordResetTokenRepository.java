package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.PasswordResetToken;

import java.util.Optional;

/**
 * Domain repository for PasswordResetToken operations.
 * Implemented by infrastructure layer adapter.
 */
public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);
}
