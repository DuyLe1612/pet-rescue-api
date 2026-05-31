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

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<String> findExpoPushTokenById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
