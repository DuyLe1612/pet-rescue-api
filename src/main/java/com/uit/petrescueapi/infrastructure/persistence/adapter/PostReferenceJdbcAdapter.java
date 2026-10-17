package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.PostReferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
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
    public Map<UUID, String> findMediaPublicIds(Collection<UUID> mediaIds) {
        Map<UUID, String> result = new HashMap<>();
        if (mediaIds == null || mediaIds.isEmpty()) {
            return result;
        }
        jdbc.query("SELECT media_id, public_id FROM media_files WHERE media_id IN (:ids) AND is_deleted = FALSE",
                Map.of("ids", Set.copyOf(mediaIds)),
                rs -> {
                    result.put(rs.getObject("media_id", UUID.class), rs.getString("public_id"));
                });
        return result;
    }

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
}
