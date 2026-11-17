package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.PostReferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Plain-JDBC reference checks for MongoDB posts (safe inside a MongoDB transaction). */
@Component
@RequiredArgsConstructor
public class PostReferenceJdbcAdapter implements PostReferencePort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Set<String> findExistingTagCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT code FROM tags WHERE code IN (:codes) AND is_deleted = FALSE",
                Map.of("codes", Set.copyOf(codes)), String.class));
    }

    @Override
    public Optional<String> findRescueCaseStatus(UUID caseId) {
        List<String> status = jdbc.queryForList("SELECT status FROM rescue_cases WHERE case_id = :id AND is_deleted = FALSE",
                Map.of("id", caseId), String.class);
        return status.stream().findFirst();
    }

    @Override
    public Map<UUID, RescueCaseCard> findRescueCaseCards(Collection<UUID> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, RescueCaseCard> cards = new java.util.HashMap<>();
        jdbc.query("""
                SELECT rc.case_id, rc.case_code, rc.status, rc.priority, rc.species, rc.location_text, rc.province_name,
                       (SELECT mf.public_id FROM rescue_media rm JOIN media_files mf ON mf.media_id = rm.media_id
                         WHERE rm.case_id = rc.case_id ORDER BY mf.created_at LIMIT 1) AS first_image
                FROM rescue_cases rc
                WHERE rc.case_id IN (:ids) AND rc.is_deleted = FALSE
                """, Map.of("ids", Set.copyOf(caseIds)), rs -> {
            UUID id = rs.getObject("case_id", UUID.class);
            cards.put(id, new RescueCaseCard(id, rs.getString("case_code"), rs.getString("status"), rs.getString("priority"),
                    rs.getString("species"), rs.getString("location_text"), rs.getString("province_name"),
                    rs.getString("first_image")));
        });
        return cards;
    }
}
