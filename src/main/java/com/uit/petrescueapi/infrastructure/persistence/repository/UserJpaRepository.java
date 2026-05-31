package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Fetch user with roles eagerly to avoid LazyInitializationException when accessed outside a session
    @Query("SELECT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<UserJpaEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.userId = :id")
    Optional<UserJpaEntity> findByIdWithRoles(@Param("id") UUID id);

    @Query("SELECT u.expoPushToken FROM UserJpaEntity u WHERE u.userId = :id")
    Optional<String> findExpoPushTokenById(@Param("id") UUID id);
}
