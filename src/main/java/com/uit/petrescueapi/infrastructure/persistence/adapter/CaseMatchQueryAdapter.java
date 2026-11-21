package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.rescue.CaseMatchResponseDto;
import com.uit.petrescueapi.application.support.DisplayName;
import com.uit.petrescueapi.application.port.out.CaseMatchQueryDataPort;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseMatchQueryAdapter implements CaseMatchQueryDataPort {

    /** The other side's point follows the same blur as the public map. */
    private static final String SELECT = """
            SELECT s.suggestion_id, s.status AS match_status, s.score, s.distance_m, s.reasons,
                   o.case_id, o.case_code, o.case_type, o.status, o.species, o.color, o.description, o.distinctive_marks,
                   o.holding_status, (o.reported_at AT TIME ZONE 'UTC') AS reported_at, u.full_name,
                   ST_Y(p.shown) AS latitude, ST_X(p.shown) AS longitude, p.blurred,
                   (SELECT mf.public_id FROM rescue_media rm JOIN media_files mf ON mf.media_id = rm.media_id
                     WHERE rm.case_id = o.case_id ORDER BY mf.created_at LIMIT 1) AS first_image
            FROM case_match_suggestions s
            JOIN rescue_cases o ON o.case_id = %s
            LEFT JOIN users u ON u.user_id = o.reported_by
            CROSS JOIN LATERAL (
                SELECT (o.case_type = 'LOST' OR (o.case_type = 'FOUND' AND o.holding_status = 'AT_HOME')) AS blurred
            ) b
            CROSS JOIN LATERAL (
                SELECT b.blurred, CASE WHEN b.blurred THEN ST_SnapToGrid(o.location, 0.003) ELSE o.location END AS shown
            ) p
            WHERE %s = :id AND s.status <> 'DISMISSED'
            ORDER BY s.status = 'PENDING' DESC, s.score DESC, s.created_at
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CloudStoragePort cloudStoragePort;

    @Override
    public List<CaseMatchResponseDto> findMatches(UUID caseId, boolean caseIsLost) {
        String sql = caseIsLost
                ? SELECT.formatted("s.other_case_id", "s.lost_case_id")
                : SELECT.formatted("s.lost_case_id", "s.other_case_id");
        return jdbc.query(sql, new MapSqlParameterSource("id", caseId), (rs, i) -> {
            boolean blurred = rs.getBoolean("blurred");
            String image = rs.getString("first_image");
            Timestamp reportedAt = rs.getTimestamp("reported_at");
            return CaseMatchResponseDto.builder()
                    .suggestionId(rs.getObject("suggestion_id", UUID.class))
                    .status(rs.getString("match_status"))
                    .score(rs.getInt("score"))
                    .distanceMeters((int) Math.max(100, Math.round(rs.getInt("distance_m") / 100.0) * 100))
                    .reasons(Arrays.asList(rs.getString("reasons").split("\n")))
                    .otherCase(CaseMatchResponseDto.OtherCase.builder()
                            .caseId(rs.getObject("case_id", UUID.class))
                            .caseCode(rs.getString("case_code"))
                            .caseType(rs.getString("case_type"))
                            .status(rs.getString("status"))
                            .species(rs.getString("species"))
                            .color(rs.getString("color"))
                            .description(rs.getString("description"))
                            .distinctiveMarks(rs.getString("distinctive_marks"))
                            .holdingStatus(rs.getString("holding_status"))
                            .latitude((Double) rs.getObject("latitude"))
                            .longitude((Double) rs.getObject("longitude"))
                            .locationBlurMeters(blurred ? 300 : null)
                            .reportedAt(reportedAt == null ? null : reportedAt.toLocalDateTime())
                            .reporterName(DisplayName.of(rs.getString("full_name")))
                            .firstImageUrl(image == null ? null : cloudStoragePort.buildUrl(image))
                            .build())
                    .build();
        });
    }
}
