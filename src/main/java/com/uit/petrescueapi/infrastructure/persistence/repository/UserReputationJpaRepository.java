package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.UserReputationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserReputationJpaEntity}.
 * Used by the <b>command-side</b> adapter for user reputation management.
 *
 * <p>{@code findById(UUID)} from {@link JpaRepository} is sufficient
 * since the primary key is the userId.</p>
 */
@Repository
public interface UserReputationJpaRepository extends JpaRepository<UserReputationJpaEntity, UUID> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = """
            INSERT INTO user_reputations (user_id, score, level, updated_at)
            VALUES (:userId, :delta, 'NEW', :now)
            ON CONFLICT (user_id) DO UPDATE
            SET score = COALESCE(user_reputations.score, 0) + EXCLUDED.score, updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void addToScore(@org.springframework.data.repository.query.Param("userId") UUID userId,
                    @org.springframework.data.repository.query.Param("delta") int delta,
                    @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
