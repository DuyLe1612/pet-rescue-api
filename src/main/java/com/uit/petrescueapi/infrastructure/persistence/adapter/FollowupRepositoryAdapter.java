package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.domain.repository.FollowupRepository;
import com.uit.petrescueapi.domain.valueobject.FollowupMilestone;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Plain JDBC: flat rows, and the job needs conditional updates that claim a row atomically. */
@Component
@RequiredArgsConstructor
public class FollowupRepositoryAdapter implements FollowupRepository {

    private static final String SELECT = """
            SELECT f.followup_id, f.application_id, f.pet_id, f.adopter_id, f.organization_id, f.milestone, f.status,
                   p.name AS pet_name, f.due_at, f.sent_at, f.responded_at, f.expires_at, f.severity,
                   f.still_has_pet, CAST(f.answers AS TEXT) AS answers
            FROM adoption_followups f JOIN pets p ON p.pet_id = f.pet_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    private final RowMapper<Row> rows = (rs, i) -> new Row(
            rs.getObject("followup_id", UUID.class), rs.getObject("application_id", UUID.class),
            rs.getObject("pet_id", UUID.class), rs.getObject("adopter_id", UUID.class),
            rs.getObject("organization_id", UUID.class), rs.getString("milestone"), rs.getString("status"),
            rs.getString("pet_name"), time(rs, "due_at"), time(rs, "sent_at"), time(rs, "responded_at"),
            time(rs, "expires_at"), rs.getString("severity"), (Boolean) rs.getObject("still_has_pet"),
            parse(rs.getString("answers")));

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    private Map<String, Object> parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            return json.readValue(text, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreadable follow-up answers", e);
        }
    }

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

    @Override
    public List<UUID> findDueIds(LocalDateTime now, int limit) {
        return jdbc.queryForList("""
                SELECT followup_id FROM adoption_followups
                WHERE status = 'SCHEDULED' AND due_at <= :now ORDER BY due_at LIMIT :limit
                """, new MapSqlParameterSource("now", Timestamp.valueOf(now)).addValue("limit", limit), UUID.class);
    }

    @Override
    public int expireSuperseded(LocalDateTime now) {
        return jdbc.update("""
                UPDATE adoption_followups f SET status = 'EXPIRED'
                WHERE f.status = 'SCHEDULED' AND f.due_at <= :now
                  AND EXISTS (SELECT 1 FROM adoption_followups later
                              WHERE later.application_id = f.application_id AND later.status = 'SCHEDULED'
                                AND later.due_at <= :now AND later.due_at > f.due_at)
                """, new MapSqlParameterSource("now", Timestamp.valueOf(now)));
    }

    @Override
    public Optional<Row> claim(UUID followupId, String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        int claimed = jdbc.update("""
                UPDATE adoption_followups SET status = 'SENT', sent_at = :now, token_hash = :hash, expires_at = :expires
                WHERE followup_id = :id AND status = 'SCHEDULED' AND due_at <= :now
                """, new MapSqlParameterSource("id", followupId).addValue("hash", tokenHash)
                .addValue("now", Timestamp.valueOf(now)).addValue("expires", Timestamp.valueOf(expiresAt)));
        return claimed == 0 ? Optional.empty() : findById(followupId);
    }

    @Override
    public Optional<Row> findByTokenHash(String tokenHash) {
        return jdbc.query(SELECT + " WHERE f.token_hash = :hash", new MapSqlParameterSource("hash", tokenHash), rows)
                .stream().findFirst();
    }

    @Override
    public Optional<Row> findById(UUID followupId) {
        return jdbc.query(SELECT + " WHERE f.followup_id = :id", new MapSqlParameterSource("id", followupId), rows)
                .stream().findFirst();
    }

    @Override
    public List<Row> findByApplication(UUID applicationId) {
        return jdbc.query(SELECT + " WHERE f.application_id = :app ORDER BY f.due_at",
                new MapSqlParameterSource("app", applicationId), rows);
    }

    @Override
    public boolean recordAnswers(UUID followupId, Map<String, Object> answers, Boolean stillHasPet, String severity, LocalDateTime now) {
        String text;
        try {
            text = json.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return jdbc.update("""
                UPDATE adoption_followups
                SET status = 'RESPONDED', responded_at = :now, answers = CAST(:answers AS JSONB),
                    still_has_pet = :still, severity = :severity
                WHERE followup_id = :id AND status = 'SENT' AND expires_at > :now
                """, new MapSqlParameterSource("id", followupId).addValue("answers", text).addValue("still", stillHasPet)
                .addValue("severity", severity).addValue("now", Timestamp.valueOf(now))) > 0;
    }

    @Override
    public List<Row> claimReminders(LocalDateTime now, LocalDateTime sentBefore) {
        List<UUID> ids = jdbc.queryForList("""
                UPDATE adoption_followups SET reminded_at = :now
                WHERE status = 'SENT' AND reminded_at IS NULL AND sent_at <= :before AND expires_at > :now
                RETURNING followup_id
                """, new MapSqlParameterSource("now", Timestamp.valueOf(now)).addValue("before", Timestamp.valueOf(sentBefore)), UUID.class);
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    public List<Row> expireOverdue(LocalDateTime now) {
        List<UUID> ids = jdbc.queryForList("""
                UPDATE adoption_followups SET status = 'EXPIRED'
                WHERE status = 'SENT' AND expires_at <= :now RETURNING followup_id
                """, new MapSqlParameterSource("now", Timestamp.valueOf(now)), UUID.class);
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    public boolean isOrganizationHandler(UUID organizationId, UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM organization_members
                WHERE organization_id = :org AND user_id = :user AND status = 'ACTIVE' AND role IN ('OWNER', 'STAFF')
                """, new MapSqlParameterSource("org", organizationId).addValue("user", userId), Integer.class);
        return n != null && n > 0;
    }

    private static final String ALERT_SELECT = """
            SELECT a.alert_id, a.followup_id, a.application_id, ap.pet_id, p.name AS pet_name, a.organization_id, a.severity,
                   a.reason_codes, a.status, a.assigned_to, a.first_action_at, a.resolved_at, a.resolution, a.note, a.created_at
            FROM followup_alerts a
            JOIN adoption_applications ap ON ap.application_id = a.application_id
            JOIN pets p ON p.pet_id = ap.pet_id
            """;

    private final RowMapper<AlertRow> alertRows = (rs, i) -> new AlertRow(
            rs.getObject("alert_id", UUID.class), rs.getObject("followup_id", UUID.class),
            rs.getObject("application_id", UUID.class), rs.getObject("pet_id", UUID.class), rs.getString("pet_name"),
            rs.getObject("organization_id", UUID.class), rs.getString("severity"),
            List.of(rs.getString("reason_codes").split(",")), rs.getString("status"),
            rs.getObject("assigned_to", UUID.class), time(rs, "first_action_at"), time(rs, "resolved_at"),
            rs.getString("resolution"), rs.getString("note"), time(rs, "created_at"));

    @Override
    public UUID createAlert(UUID followupId, UUID applicationId, UUID organizationId, String severity, List<String> reasons) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO followup_alerts (alert_id, followup_id, application_id, organization_id, severity, reason_codes)
                VALUES (:id, :followup, :app, :org, :severity, :reasons)
                """, new MapSqlParameterSource("id", id).addValue("followup", followupId).addValue("app", applicationId)
                .addValue("org", organizationId).addValue("severity", severity).addValue("reasons", String.join(",", reasons)));
        return id;
    }

    @Override
    public List<AlertRow> findAlerts(UUID organizationId, String status, int limit) {
        return jdbc.query(ALERT_SELECT + """
                WHERE (CAST(:org AS UUID) IS NULL OR a.organization_id = :org)
                  AND (CAST(:status AS VARCHAR) IS NULL OR a.status = :status)
                ORDER BY CASE a.severity WHEN 'RED' THEN 0 ELSE 1 END, a.created_at
                LIMIT :limit
                """, new MapSqlParameterSource("org", organizationId).addValue("status", status).addValue("limit", limit), alertRows);
    }

    @Override
    public Optional<AlertRow> findAlert(UUID alertId) {
        return jdbc.query(ALERT_SELECT + " WHERE a.alert_id = :id", new MapSqlParameterSource("id", alertId), alertRows)
                .stream().findFirst();
    }

    @Override
    public void updateAlert(UUID alertId, String status, UUID assignedTo, String note, String resolution, LocalDateTime now) {
        jdbc.update("""
                UPDATE followup_alerts
                SET status = :status,
                    assigned_to = COALESCE(assigned_to, :user),
                    first_action_at = COALESCE(first_action_at, CASE WHEN :status <> 'OPEN' THEN CAST(:now AS TIMESTAMPTZ) END),
                    resolved_at = CASE WHEN :status = 'RESOLVED' THEN CAST(:now AS TIMESTAMPTZ) ELSE NULL END,
                    resolution = CASE WHEN :status = 'RESOLVED' THEN :resolution ELSE NULL END,
                    note = COALESCE(:note, note)
                WHERE alert_id = :id
                """, new MapSqlParameterSource("id", alertId).addValue("status", status).addValue("user", assignedTo)
                .addValue("note", note).addValue("resolution", resolution).addValue("now", Timestamp.valueOf(now)));
    }

    @Override
    public Optional<UUID> findHandlerOrganizationId(UUID userId) {
        return jdbc.queryForList("""
                SELECT organization_id FROM organization_members
                WHERE user_id = :user AND status = 'ACTIVE' AND role IN ('OWNER', 'STAFF') LIMIT 1
                """, new MapSqlParameterSource("user", userId), UUID.class).stream().findFirst();
    }

    @Override
    public Optional<Contact> findContact(UUID userId) {
        return jdbc.query("SELECT email, COALESCE(NULLIF(full_name, ''), username) AS name FROM users WHERE user_id = :id",
                new MapSqlParameterSource("id", userId), (rs, i) -> new Contact(rs.getString("email"), rs.getString("name")))
                .stream().findFirst();
    }
}
