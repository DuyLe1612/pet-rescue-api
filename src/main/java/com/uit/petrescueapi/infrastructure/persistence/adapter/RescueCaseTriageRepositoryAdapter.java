package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCaseTriage;
import com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.domain.valueobject.TriageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Plain JDBC: one flat row per case, and the claim needs a single atomic statement. */
@Component
@RequiredArgsConstructor
public class RescueCaseTriageRepositoryAdapter implements RescueCaseTriageRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<RescueCaseTriage> findByCaseId(UUID caseId) {
        return jdbc.query("SELECT * FROM rescue_case_triage WHERE case_id = :id", new MapSqlParameterSource("id", caseId), MAPPER)
                .stream().findFirst();
    }

    @Override
    public boolean claim(UUID caseId, int maxAttempts, LocalDateTime staleBefore) {
        // The WHERE of the conflict branch is the whole guard: no row is touched, so nothing is
        // counted as updated, when the case is done, out of attempts or held by a live worker.
        return jdbc.update("""
                INSERT INTO rescue_case_triage (case_id, status, attempts, claimed_at)
                VALUES (:id, 'PENDING', 1, NOW())
                ON CONFLICT (case_id) DO UPDATE
                   SET status = 'PENDING', attempts = rescue_case_triage.attempts + 1,
                       claimed_at = NOW(), updated_at = NOW()
                 WHERE rescue_case_triage.status <> 'ANALYZED'
                   AND rescue_case_triage.attempts < :max
                   AND (rescue_case_triage.claimed_at IS NULL OR rescue_case_triage.claimed_at < :stale)
                """, new MapSqlParameterSource()
                .addValue("id", caseId)
                .addValue("max", maxAttempts)
                .addValue("stale", Timestamp.valueOf(staleBefore))) == 1;
    }

    @Override
    public void markAnalyzed(RescueCaseTriage r) {
        jdbc.update("""
                UPDATE rescue_case_triage
                   SET status = 'ANALYZED', claimed_at = NULL, failure = NULL, model = :model, prompt_version = :promptVersion,
                       ai_urgency = :urgency, needs_rescue = :needsRescue, condition_summary = :summary, species = :species,
                       district = :district, province = :province, missing_fields = :missing, latency_ms = :latency,
                       input_tokens = :inputTokens, output_tokens = :outputTokens, raw_output = :raw,
                       analyzed_at = NOW(), updated_at = NOW()
                 WHERE case_id = :id
                """, new MapSqlParameterSource()
                .addValue("id", r.getCaseId())
                .addValue("model", r.getModel())
                .addValue("promptVersion", r.getPromptVersion())
                .addValue("urgency", r.getAiUrgency() == null ? null : r.getAiUrgency().name())
                .addValue("needsRescue", r.getNeedsRescue())
                .addValue("summary", r.getConditionSummary())
                .addValue("species", r.getSpecies())
                .addValue("district", r.getDistrict())
                .addValue("province", r.getProvince())
                .addValue("missing", r.getMissingFields() == null || r.getMissingFields().isEmpty()
                        ? null : String.join(",", r.getMissingFields()))
                .addValue("latency", r.getLatencyMs())
                .addValue("inputTokens", r.getInputTokens())
                .addValue("outputTokens", r.getOutputTokens())
                .addValue("raw", r.getRawOutput()));
    }

    @Override
    public void markFailed(UUID caseId, String failure, String rawOutput, Long latencyMs) {
        jdbc.update("""
                UPDATE rescue_case_triage
                   SET status = 'FAILED', claimed_at = NULL, failure = :failure, raw_output = :raw,
                       latency_ms = :latency, updated_at = NOW()
                 WHERE case_id = :id
                """, new MapSqlParameterSource()
                .addValue("id", caseId)
                .addValue("failure", failure)
                .addValue("raw", rawOutput)
                .addValue("latency", latencyMs));
    }

    @Override
    public List<UUID> findRetryable(int maxAttempts, LocalDateTime idleBefore, LocalDateTime reportedAfter, int limit) {
        return jdbc.queryForList("""
                SELECT rc.case_id
                FROM rescue_cases rc
                LEFT JOIN rescue_case_triage t ON t.case_id = rc.case_id
                WHERE rc.is_deleted = FALSE
                  AND rc.status IN ('REPORTED', 'IN_PROGRESS')
                  AND rc.reported_at > :reportedAfter
                  -- a brand new case still has its event-driven attempt on the way
                  AND rc.reported_at < :idleBefore
                  AND (t.case_id IS NULL
                       OR (t.status IN ('PENDING', 'FAILED') AND t.attempts < :max
                           AND t.updated_at < :idleBefore
                           AND (t.claimed_at IS NULL OR t.claimed_at < :idleBefore)))
                ORDER BY rc.reported_at
                LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("max", maxAttempts)
                .addValue("idleBefore", Timestamp.valueOf(idleBefore))
                .addValue("reportedAfter", Timestamp.valueOf(reportedAfter))
                .addValue("limit", limit), UUID.class);
    }

    private static final RowMapper<RescueCaseTriage> MAPPER = (rs, row) -> RescueCaseTriage.builder()
            .caseId(rs.getObject("case_id", UUID.class))
            .status(TriageStatus.valueOf(rs.getString("status")))
            .attempts(rs.getInt("attempts"))
            .model(rs.getString("model"))
            .promptVersion(AdopterProfileRepositoryAdapter.integer(rs, "prompt_version"))
            .aiUrgency(rs.getString("ai_urgency") == null ? null : RescuePriority.valueOf(rs.getString("ai_urgency")))
            .needsRescue(rs.getObject("needs_rescue", Boolean.class))
            .conditionSummary(rs.getString("condition_summary"))
            .species(rs.getString("species"))
            .district(rs.getString("district"))
            .province(rs.getString("province"))
            .missingFields(rs.getString("missing_fields") == null ? List.of() : Arrays.asList(rs.getString("missing_fields").split(",")))
            .failure(rs.getString("failure"))
            .rawOutput(rs.getString("raw_output"))
            .latencyMs(rs.getObject("latency_ms", Long.class))
            .inputTokens(rs.getObject("input_tokens", Long.class))
            .outputTokens(rs.getObject("output_tokens", Long.class))
            .analyzedAt(rs.getTimestamp("analyzed_at") == null ? null : rs.getTimestamp("analyzed_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();
}
