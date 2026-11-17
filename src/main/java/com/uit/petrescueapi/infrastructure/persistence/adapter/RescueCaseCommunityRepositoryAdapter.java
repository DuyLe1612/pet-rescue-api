package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.repository.RescueCaseCommunityRepository;
import com.uit.petrescueapi.domain.valueobject.RescueCaseFlagReason;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Plain JDBC: flat rows, and the flag needs a single upsert. */
@Component
@RequiredArgsConstructor
public class RescueCaseCommunityRepositoryAdapter implements RescueCaseCommunityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void saveUpdate(UUID updateId, UUID caseId, UUID authorId, String note, List<UUID> mediaIds) {
        jdbc.update("""
                INSERT INTO rescue_case_updates (update_id, case_id, author_id, note)
                VALUES (:id, :caseId, :authorId, :note)
                """, new MapSqlParameterSource()
                .addValue("id", updateId)
                .addValue("caseId", caseId)
                .addValue("authorId", authorId)
                .addValue("note", note));
        for (int i = 0; i < mediaIds.size(); i++) {
            jdbc.update("""
                    INSERT INTO rescue_case_update_media (update_id, media_id, position)
                    VALUES (:id, :mediaId, :position)
                    """, new MapSqlParameterSource()
                    .addValue("id", updateId)
                    .addValue("mediaId", mediaIds.get(i))
                    .addValue("position", i));
        }
    }

    @Override
    public void saveFlag(UUID caseId, UUID userId, RescueCaseFlagReason reason, String note) {
        jdbc.update("""
                INSERT INTO rescue_case_flags (case_id, user_id, reason, note)
                VALUES (:caseId, :userId, :reason, :note)
                ON CONFLICT (case_id, user_id) DO UPDATE SET reason = :reason, note = :note
                """, new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("userId", userId)
                .addValue("reason", reason.name())
                .addValue("note", note));
    }

    @Override
    public void deleteFlag(UUID caseId, UUID userId) {
        jdbc.update("DELETE FROM rescue_case_flags WHERE case_id = :caseId AND user_id = :userId",
                new MapSqlParameterSource().addValue("caseId", caseId).addValue("userId", userId));
    }

    @Override
    public int countFlags(UUID caseId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM rescue_case_flags WHERE case_id = :caseId",
                new MapSqlParameterSource("caseId", caseId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public boolean markFlagAlerted(UUID caseId) {
        return jdbc.update("UPDATE rescue_cases SET flag_alerted_at = NOW() WHERE case_id = :caseId AND flag_alerted_at IS NULL",
                new MapSqlParameterSource("caseId", caseId)) == 1;
    }
}
