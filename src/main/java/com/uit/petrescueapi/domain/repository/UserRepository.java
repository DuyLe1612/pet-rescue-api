package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the User aggregate.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    /** Existence check without loading the aggregate (no lazy roles). */
    boolean existsById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<String> findExpoPushTokenById(UUID id);

    /** Token per user; users without a token are absent. */
    java.util.Map<UUID, String> findExpoPushTokensByIds(java.util.Collection<UUID> ids);

    boolean hasRole(UUID id, String roleCode);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
