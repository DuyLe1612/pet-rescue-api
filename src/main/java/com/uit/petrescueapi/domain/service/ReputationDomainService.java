package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.UserReputation;
import com.uit.petrescueapi.domain.repository.UserReputationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** Changes a user's reputation score and records why. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReputationDomainService {

    private final UserReputationRepository userReputationRepository;
    private final JdbcTemplate jdbcTemplate;

    public void adjust(UUID userId, int delta, String reason, String referenceType, UUID referenceId, UUID actorId) {
        UserReputation reputation = userReputationRepository.findByUserId(userId)
                .orElse(UserReputation.builder()
                        .userId(userId)
                        .score(0)
                        .level("NEW")
                        .build());
        reputation.setScore((reputation.getScore() == null ? 0 : reputation.getScore()) + delta);
        reputation.setUpdatedAt(LocalDateTime.now());
        userReputationRepository.save(reputation);
        jdbcTemplate.update("""
                INSERT INTO reputation_logs (log_id, user_id, delta, reason, reference_type, reference_id, created_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                userId,
                delta,
                reason,
                referenceType,
                referenceId,
                LocalDateTime.now(),
                actorId);
    }

    /** Limits score farming between accounts: past the daily limit the event earns nothing. */
    public void adjustWithDailyLimit(UUID userId, int delta, String reason, String referenceType, UUID referenceId,
                                     UUID actorId, int dailyLimit) {
        Long earnedToday = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reputation_logs
                WHERE user_id = ? AND reason = ? AND created_at >= ?
                """, Long.class, userId, reason, LocalDateTime.now().minusDays(1));
        if (earnedToday != null && earnedToday >= dailyLimit) {
            return;
        }
        adjust(userId, delta, reason, referenceType, referenceId, actorId);
    }
}
