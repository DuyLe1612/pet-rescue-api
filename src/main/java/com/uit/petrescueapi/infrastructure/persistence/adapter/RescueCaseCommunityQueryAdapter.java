package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.rescue.FlaggedRescueCaseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseUpdateResponseDto;
import com.uit.petrescueapi.application.support.DisplayName;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.RescueCaseCommunityQueryDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Reads progress notes and flags. Images of a page come in one statement. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescueCaseCommunityQueryAdapter implements RescueCaseCommunityQueryDataPort {

    private static final String UPDATE_SELECT = """
            SELECT up.update_id, up.case_id, up.author_id, u.full_name, u.avatar_url, up.note,
                   (up.created_at AT TIME ZONE 'UTC') AS created_at
            FROM rescue_case_updates up
            LEFT JOIN users u ON u.user_id = up.author_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CloudStoragePort cloudStoragePort;

    @Override
    public Page<RescueCaseUpdateResponseDto> findUpdates(UUID caseId, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        List<RescueCaseUpdateResponseDto> rows = jdbc.query(UPDATE_SELECT + """
                WHERE up.case_id = :caseId
                ORDER BY up.created_at DESC, up.update_id
                LIMIT :limit OFFSET :offset
                """, params, (rs, i) -> toUpdate(rs));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM rescue_case_updates WHERE case_id = :caseId", params, Long.class);
        return new PageImpl<>(withImages(rows), pageable, total == null ? 0 : total);
    }

    @Override
    public Optional<RescueCaseUpdateResponseDto> findUpdate(UUID updateId) {
        List<RescueCaseUpdateResponseDto> rows = jdbc.query(UPDATE_SELECT + " WHERE up.update_id = :id",
                new MapSqlParameterSource("id", updateId), (rs, i) -> toUpdate(rs));
        return withImages(rows).stream().findFirst();
    }

    @Override
    public Page<FlaggedRescueCaseDto> findFlaggedCases(Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        List<FlaggedRescueCaseDto> rows = jdbc.query("""
                SELECT rc.case_id, rc.case_code, rc.status, rc.species, rc.location_text, rc.reported_by, u.full_name,
                       (rc.reported_at AT TIME ZONE 'UTC') AS reported_at,
                       COUNT(*) AS flag_count,
                       COUNT(*) FILTER (WHERE f.reason = 'FALSE_REPORT') AS false_report_count,
                       COUNT(*) FILTER (WHERE f.reason = 'ALREADY_HANDLED') AS already_handled_count,
                       COUNT(*) FILTER (WHERE f.reason = 'DUPLICATE') AS duplicate_count,
                       (MAX(f.created_at) AT TIME ZONE 'UTC') AS last_flagged_at
                FROM rescue_case_flags f
                JOIN rescue_cases rc ON rc.case_id = f.case_id
                LEFT JOIN users u ON u.user_id = rc.reported_by
                WHERE rc.is_deleted = false AND rc.status IN ('REPORTED', 'IN_PROGRESS')
                GROUP BY rc.case_id, u.full_name
                ORDER BY flag_count DESC, last_flagged_at DESC
                LIMIT :limit OFFSET :offset
                """, params, (rs, i) -> FlaggedRescueCaseDto.builder()
                .caseId(rs.getObject("case_id", UUID.class))
                .caseCode(rs.getString("case_code"))
                .status(rs.getString("status"))
                .species(rs.getString("species"))
                .locationText(rs.getString("location_text"))
                .reporterId(rs.getObject("reported_by", UUID.class))
                .reporterName(DisplayName.of(rs.getString("full_name")))
                .reportedAt(toLocal(rs.getTimestamp("reported_at")))
                .flagCount(rs.getInt("flag_count"))
                .falseReportCount(rs.getInt("false_report_count"))
                .alreadyHandledCount(rs.getInt("already_handled_count"))
                .duplicateCount(rs.getInt("duplicate_count"))
                .lastFlaggedAt(toLocal(rs.getTimestamp("last_flagged_at")))
                .build());
        Long total = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT f.case_id)
                FROM rescue_case_flags f
                JOIN rescue_cases rc ON rc.case_id = f.case_id
                WHERE rc.is_deleted = false AND rc.status IN ('REPORTED', 'IN_PROGRESS')
                """, params, Long.class);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    @Override
    public boolean hasFlagged(UUID caseId, UUID userId) {
        if (userId == null) {
            return false;
        }
        Boolean exists = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM rescue_case_flags WHERE case_id = :caseId AND user_id = :userId)",
                new MapSqlParameterSource().addValue("caseId", caseId).addValue("userId", userId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private List<RescueCaseUpdateResponseDto> withImages(List<RescueCaseUpdateResponseDto> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<UUID, List<String>> images = new HashMap<>();
        jdbc.query("""
                SELECT um.update_id, mf.public_id
                FROM rescue_case_update_media um
                JOIN media_files mf ON mf.media_id = um.media_id
                WHERE um.update_id IN (:ids)
                ORDER BY um.position
                """, new MapSqlParameterSource("ids", rows.stream().map(RescueCaseUpdateResponseDto::getUpdateId).toList()),
                rs -> {
                    images.computeIfAbsent(rs.getObject("update_id", UUID.class), id -> new ArrayList<>())
                            .add(cloudStoragePort.buildUrl(rs.getString("public_id")));
                });
        rows.forEach(row -> row.setImageUrls(images.getOrDefault(row.getUpdateId(), List.of())));
        return rows;
    }

    private static RescueCaseUpdateResponseDto toUpdate(ResultSet rs) throws SQLException {
        return RescueCaseUpdateResponseDto.builder()
                .updateId(rs.getObject("update_id", UUID.class))
                .caseId(rs.getObject("case_id", UUID.class))
                .authorId(rs.getObject("author_id", UUID.class))
                .authorName(DisplayName.of(rs.getString("full_name")))
                .authorAvatarUrl(rs.getString("avatar_url"))
                .note(rs.getString("note"))
                .createdAt(toLocal(rs.getTimestamp("created_at")))
                .build();
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
