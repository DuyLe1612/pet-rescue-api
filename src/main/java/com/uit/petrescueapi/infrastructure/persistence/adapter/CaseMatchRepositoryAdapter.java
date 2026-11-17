package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.repository.CaseMatchRepository;
import com.uit.petrescueapi.domain.service.CaseMatchRules;
import com.uit.petrescueapi.domain.valueobject.CaseMatchCandidate;
import com.uit.petrescueapi.domain.valueobject.CaseMatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Plain JDBC: the pairing is one spatial self-join. */
@Component
@RequiredArgsConstructor
public class CaseMatchRepositoryAdapter implements CaseMatchRepository {

    /**
     * A LOST case still open, paired with an open FOUND case or a rescue not yet closed, by someone
     * else, of the same species or an unknown one, reported from a day before the pet was lost up
     * to 30 days after, within the lost animal's radius.
     */
    private static final String PAIRS = """
            SELECT lost.case_id AS lost_id, other.case_id AS other_id, lost.reported_by AS lost_reporter,
                   ST_Distance(lost.location::geography, other.location::geography) AS distance,
                   lost.species AS lost_species, other.species AS other_species,
                   lost.color AS lost_color, lost.distinctive_marks AS lost_marks, lost.description AS lost_description,
                   other.color AS other_color, other.description AS other_description, other.distinctive_marks AS other_marks,
                   EXTRACT(EPOCH FROM (other.reported_at - COALESCE(lost.last_seen_at, lost.reported_at))) / 3600.0 AS hours_after
            FROM rescue_cases lost
            JOIN rescue_cases other
              ON other.case_id <> lost.case_id
             AND other.is_deleted = false
             AND other.location IS NOT NULL
             AND ((other.case_type = 'FOUND' AND other.status IN ('REPORTED', 'IN_PROGRESS'))
               OR (other.case_type = 'RESCUE' AND other.status IN ('REPORTED', 'IN_PROGRESS', 'RESCUED')))
             AND other.reported_by IS DISTINCT FROM lost.reported_by
             AND (lost.species IS NULL OR other.species IS NULL OR LOWER(lost.species) = LOWER(other.species))
             AND other.reported_at >= COALESCE(lost.last_seen_at, lost.reported_at) - INTERVAL '1 day'
             AND other.reported_at <= COALESCE(lost.last_seen_at, lost.reported_at) + make_interval(days => :windowDays)
             AND ST_DWithin(lost.location::geography, other.location::geography,
                    CASE WHEN LOWER(lost.species) IN ('dog', 'chó', 'cho') THEN :dogRadius
                         WHEN LOWER(lost.species) IN ('cat', 'mèo', 'meo') THEN :catRadius
                         ELSE :otherRadius END)
            WHERE lost.case_type = 'LOST'
              AND lost.status IN ('REPORTED', 'IN_PROGRESS')
              AND lost.is_deleted = false
              AND lost.location IS NOT NULL
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<CaseMatchCandidate> findCandidates(UUID caseId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", caseId)
                .addValue("windowDays", CaseMatchRules.LOST_WINDOW_DAYS)
                .addValue("dogRadius", CaseMatchRules.DOG_RADIUS_M)
                .addValue("catRadius", CaseMatchRules.CAT_RADIUS_M)
                .addValue("otherRadius", CaseMatchRules.OTHER_RADIUS_M);
        return jdbc.query(PAIRS + " AND (lost.case_id = :id OR other.case_id = :id) ORDER BY distance LIMIT 20", params,
                (rs, i) -> new CaseMatchCandidate(
                        rs.getObject("lost_id", UUID.class),
                        rs.getObject("other_id", UUID.class),
                        rs.getObject("lost_reporter", UUID.class),
                        rs.getDouble("distance"),
                        rs.getString("lost_species"),
                        rs.getString("other_species"),
                        rs.getString("lost_color"),
                        rs.getString("lost_marks"),
                        rs.getString("lost_description"),
                        rs.getString("other_color"),
                        rs.getString("other_description"),
                        rs.getString("other_marks"),
                        rs.getObject("hours_after") == null ? null : rs.getDouble("hours_after")));
    }

    @Override
    public boolean saveSuggestion(UUID suggestionId, UUID lostCaseId, UUID otherCaseId, int distanceMeters, int score, List<String> reasons) {
        return jdbc.update("""
                INSERT INTO case_match_suggestions (suggestion_id, lost_case_id, other_case_id, distance_m, score, reasons)
                VALUES (:id, :lost, :other, :distance, :score, :reasons)
                ON CONFLICT (lost_case_id, other_case_id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", suggestionId)
                .addValue("lost", lostCaseId)
                .addValue("other", otherCaseId)
                .addValue("distance", distanceMeters)
                .addValue("score", score)
                .addValue("reasons", String.join("\n", reasons))) == 1;
    }

    @Override
    public Optional<Suggestion> findSuggestion(UUID suggestionId) {
        return jdbc.query("SELECT suggestion_id, lost_case_id, other_case_id, status FROM case_match_suggestions WHERE suggestion_id = :id",
                new MapSqlParameterSource("id", suggestionId),
                (rs, i) -> new Suggestion(rs.getObject("suggestion_id", UUID.class), rs.getObject("lost_case_id", UUID.class),
                        rs.getObject("other_case_id", UUID.class), CaseMatchStatus.valueOf(rs.getString("status"))))
                .stream().findFirst();
    }

    @Override
    public void decide(UUID suggestionId, CaseMatchStatus status, UUID decidedBy) {
        jdbc.update("UPDATE case_match_suggestions SET status = :status, decided_by = :by, decided_at = NOW() WHERE suggestion_id = :id",
                new MapSqlParameterSource().addValue("id", suggestionId).addValue("status", status.name()).addValue("by", decidedBy));
    }

    @Override
    public void dismissPending(UUID lostCaseId, UUID except) {
        jdbc.update("""
                UPDATE case_match_suggestions SET status = 'DISMISSED', decided_at = NOW()
                 WHERE lost_case_id = :lost AND status = 'PENDING' AND suggestion_id IS DISTINCT FROM :except
                """, new MapSqlParameterSource().addValue("lost", lostCaseId).addValue("except", except));
    }

    @Override
    public Optional<UUID> findOpenLostCaseOfPet(UUID petId) {
        return jdbc.queryForList("""
                SELECT case_id FROM rescue_cases
                 WHERE pet_id = :pet AND case_type = 'LOST' AND status IN ('REPORTED', 'IN_PROGRESS') AND is_deleted = false
                 LIMIT 1
                """, new MapSqlParameterSource("pet", petId), UUID.class).stream().findFirst();
    }
}
