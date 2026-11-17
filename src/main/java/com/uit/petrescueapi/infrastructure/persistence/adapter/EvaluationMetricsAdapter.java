package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.admin.EvaluationMetricsDto;
import com.uit.petrescueapi.application.port.out.EvaluationMetricsDataPort;
import com.uit.petrescueapi.domain.valueobject.MatchLevel;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Aggregates in the database, one statement per group of numbers. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationMetricsAdapter implements EvaluationMetricsDataPort {

    private static final String RANK = "CASE %s WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'NORMAL' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END";
    private static final String DATA_NOTE = "Counts every row in the database, demo data included.";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public EvaluationMetricsDto.Matching matching() {
        MapSqlParameterSource none = new MapSqlParameterSource();

        Map<String, long[]> levels = new HashMap<>();
        jdbc.query("""
                SELECT match_level,
                       COUNT(*) AS submitted,
                       COUNT(*) FILTER (WHERE status IN ('APPROVED', 'REJECTED', 'COMPLETED')) AS decided,
                       COUNT(*) FILTER (WHERE status IN ('APPROVED', 'COMPLETED')) AS approved
                FROM adoption_applications
                WHERE is_deleted = false AND match_level IS NOT NULL
                GROUP BY match_level
                """, none, rs -> {
            levels.put(rs.getString("match_level"), new long[]{rs.getLong("submitted"), rs.getLong("decided"), rs.getLong("approved")});
        });

        Map<String, Map<String, Long>> outcomes = new HashMap<>();
        jdbc.query("""
                SELECT a.match_level, o.outcome_type, COUNT(*) AS n
                FROM adoption_outcomes o
                JOIN adoption_applications a ON a.application_id = o.application_id
                WHERE a.match_level IS NOT NULL
                GROUP BY a.match_level, o.outcome_type
                """, none, rs -> {
            outcomes.computeIfAbsent(rs.getString("match_level"), k -> new TreeMap<>()).put(rs.getString("outcome_type"), rs.getLong("n"));
        });

        List<EvaluationMetricsDto.Level> byLevel = new ArrayList<>();
        for (MatchLevel level : MatchLevel.values()) {
            long[] n = levels.getOrDefault(level.name(), new long[3]);
            byLevel.add(new EvaluationMetricsDto.Level(level.name(), n[0], n[1], n[2], rate(n[2], n[1]),
                    outcomes.getOrDefault(level.name(), Map.of())));
        }

        EvaluationMetricsDto.Warnings warnings = jdbc.queryForObject("""
                SELECT COUNT(*) AS scored,
                       COUNT(*) FILTER (WHERE w) AS with_warnings,
                       COUNT(*) FILTER (WHERE w AND status IN ('APPROVED', 'REJECTED', 'COMPLETED')) AS decided_warned,
                       COUNT(*) FILTER (WHERE w AND status IN ('APPROVED', 'COMPLETED')) AS approved_warned
                FROM (SELECT status, COALESCE(match_warnings, '') <> '' AS w
                      FROM adoption_applications
                      WHERE is_deleted = false AND match_level IS NOT NULL) a
                """, none, (rs, i) -> new EvaluationMetricsDto.Warnings(
                rs.getLong("scored"), rs.getLong("with_warnings"), rate(rs.getLong("with_warnings"), rs.getLong("scored")),
                rs.getLong("decided_warned"), rs.getLong("approved_warned"),
                rate(rs.getLong("approved_warned"), rs.getLong("decided_warned"))));

        EvaluationMetricsDto.TraitCoverage traits = jdbc.queryForObject("""
                SELECT COUNT(*) AS available, COUNT(t.pet_id) AS with_traits
                FROM pets p LEFT JOIN pet_traits t ON t.pet_id = p.pet_id
                WHERE p.is_deleted = false AND p.status = 'AVAILABLE'
                """, none, (rs, i) -> new EvaluationMetricsDto.TraitCoverage(
                rs.getLong("available"), rs.getLong("with_traits"), rate(rs.getLong("with_traits"), rs.getLong("available"))));

        return new EvaluationMetricsDto.Matching(byLevel, warnings, traits, DATA_NOTE
                + " Consistency of the score is checked by tests, not here: a profile edited after applying would differ.");
    }

    @Override
    public EvaluationMetricsDto.Rescue rescue() {
        MapSqlParameterSource none = new MapSqlParameterSource();

        Map<String, EvaluationMetricsDto.ClaimTime> claimRows = new HashMap<>();
        jdbc.query("""
                WITH c AS (
                    SELECT rc.priority, COALESCE(t.status = 'ANALYZED', false) AS triaged, rc.reported_at,
                           (SELECT MIN(cl.claimed_at) FROM rescue_case_claims cl WHERE cl.case_id = rc.case_id) AS first_claim
                    FROM rescue_cases rc
                    LEFT JOIN rescue_case_triage t ON t.case_id = rc.case_id
                    WHERE rc.is_deleted = false AND rc.case_type = 'RESCUE' AND rc.priority IS NOT NULL
                )
                SELECT priority, triaged, COUNT(*) AS cases, COUNT(first_claim) AS claimed,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM first_claim - reported_at) / 60.0)
                           FILTER (WHERE first_claim IS NOT NULL) AS median_minutes
                FROM c
                GROUP BY priority, triaged
                """, none, rs -> {
            Object median = rs.getObject("median_minutes");
            claimRows.put(rs.getString("priority") + "/" + rs.getBoolean("triaged"), new EvaluationMetricsDto.ClaimTime(
                    rs.getString("priority"), rs.getBoolean("triaged"), rs.getLong("cases"), rs.getLong("claimed"),
                    median == null ? null : round1(((Number) median).doubleValue())));
        });
        List<EvaluationMetricsDto.ClaimTime> timeToFirstClaim = new ArrayList<>();
        for (RescuePriority priority : RescuePriority.values()) {
            for (boolean triaged : new boolean[]{false, true}) {
                timeToFirstClaim.add(claimRows.getOrDefault(priority.name() + "/" + triaged,
                        new EvaluationMetricsDto.ClaimTime(priority.name(), triaged, 0, 0, null)));
            }
        }

        String ai = RANK.formatted("t.ai_urgency");
        String set = RANK.formatted("rc.priority");
        EvaluationMetricsDto.Triage triage = jdbc.queryForObject("""
                SELECT COUNT(*) AS analyzed,
                       COUNT(*) FILTER (WHERE t.ai_urgency = rc.priority) AS agree,
                       COUNT(*) FILTER (WHERE t.ai_urgency IS NOT NULL AND %1$s > %2$s) AS ai_higher,
                       COUNT(*) FILTER (WHERE t.ai_urgency IS NOT NULL AND %1$s < %2$s) AS ai_lower,
                       COUNT(*) FILTER (WHERE t.ai_urgency IS NULL) AS ai_none,
                       COUNT(*) FILTER (WHERE rc.priority = 'CRITICAL' AND t.ai_urgency IS DISTINCT FROM 'CRITICAL') AS critical_missed
                FROM rescue_case_triage t
                JOIN rescue_cases rc ON rc.case_id = t.case_id
                WHERE t.status = 'ANALYZED' AND rc.is_deleted = false
                """.formatted(ai, set), none, (rs, i) -> new EvaluationMetricsDto.Triage(
                rs.getLong("analyzed"), rs.getLong("agree"), rs.getLong("ai_higher"), rs.getLong("ai_lower"),
                rs.getLong("ai_none"), rs.getLong("critical_missed")));

        long[] suggestions = jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'PENDING') AS pending,
                       COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed,
                       COUNT(*) FILTER (WHERE status = 'REJECTED') AS rejected
                FROM case_match_suggestions
                """, none, (rs, i) -> new long[]{rs.getLong("total"), rs.getLong("pending"), rs.getLong("confirmed"), rs.getLong("rejected")});

        EvaluationMetricsDto.LostAndFound lostAndFound = jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE case_type = 'LOST' AND status IN ('REPORTED', 'IN_PROGRESS')) AS open_lost,
                       COUNT(*) FILTER (WHERE case_type = 'FOUND' AND status IN ('REPORTED', 'IN_PROGRESS')) AS open_found,
                       COUNT(*) FILTER (WHERE case_type = 'LOST' AND resolution = 'REUNITED') AS reunited,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM resolved_at - reported_at) / 3600.0)
                           FILTER (WHERE case_type = 'LOST' AND resolution = 'REUNITED' AND resolved_at IS NOT NULL) AS median_hours
                FROM rescue_cases
                WHERE is_deleted = false
                """, none, (rs, i) -> {
            Object median = rs.getObject("median_hours");
            return new EvaluationMetricsDto.LostAndFound(rs.getLong("open_lost"), rs.getLong("open_found"),
                    suggestions[0], suggestions[1], suggestions[2], suggestions[3],
                    rate(suggestions[2], suggestions[2] + suggestions[3]), rs.getLong("reunited"),
                    median == null ? null : round1(((Number) median).doubleValue()));
        });

        Long flagged = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT f.case_id)
                FROM rescue_case_flags f JOIN rescue_cases rc ON rc.case_id = f.case_id
                WHERE rc.is_deleted = false AND rc.status IN ('REPORTED', 'IN_PROGRESS')
                """, none, Long.class);

        return new EvaluationMetricsDto.Rescue(timeToFirstClaim, triage, lostAndFound, flagged == null ? 0 : flagged, DATA_NOTE
                + " Duplicate precision and recall need the merge API, which is not built.");
    }

    private static Double rate(long part, long whole) {
        return whole == 0 ? null : round1(100.0 * part / whole);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
