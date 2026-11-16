package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.repository.FollowupRepository;
import com.uit.petrescueapi.domain.valueobject.FollowupMilestone;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Plain JDBC: flat rows, and the job needs conditional updates that claim a row atomically. */
@Component
@RequiredArgsConstructor
public class FollowupRepositoryAdapter implements FollowupRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void schedule(UUID applicationId, LocalDateTime completedAt, List<Duration> offsets) {
        jdbc.update("UPDATE adoption_applications SET completed_at = :at WHERE application_id = :app",
                new MapSqlParameterSource("at", Timestamp.valueOf(completedAt)).addValue("app", applicationId));
        FollowupMilestone[] milestones = FollowupMilestone.values();
        for (int i = 0; i < offsets.size() && i < milestones.length; i++) {
            jdbc.update("""
                    INSERT INTO adoption_followups (followup_id, application_id, pet_id, adopter_id, organization_id, milestone, status, due_at)
                    SELECT :id, a.application_id, a.pet_id, a.applicant_id, a.organization_id, :milestone, 'SCHEDULED', :due
                    FROM adoption_applications a WHERE a.application_id = :app
                    ON CONFLICT (application_id, milestone) DO NOTHING
                    """, new MapSqlParameterSource("id", UUID.randomUUID()).addValue("app", applicationId)
                    .addValue("milestone", milestones[i].name())
                    .addValue("due", Timestamp.valueOf(completedAt.plus(offsets.get(i)))));
        }
    }

    @Override
    public int cancelOpen(UUID applicationId) {
        return jdbc.update("""
                UPDATE adoption_followups SET status = 'CANCELLED'
                WHERE application_id = :app AND status IN ('SCHEDULED', 'SENT')
                """, new MapSqlParameterSource("app", applicationId));
    }
}
