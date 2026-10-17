package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Plain-JDBC user lookup for MongoDB snapshots: one {@code IN} query, no entity graph,
 * so it works inside a MongoDB transaction without a JPA session.
 */
@Component
@RequiredArgsConstructor
public class UserSnapshotJdbcAdapter implements UserSnapshotPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<UUID, UserSnapshot> findByIds(Collection<UUID> ids) {
        Map<UUID, UserSnapshot> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        jdbc.query("SELECT user_id, username, full_name, avatar_url FROM users WHERE user_id IN (:ids)",
                Map.of("ids", Set.copyOf(ids)),
                rs -> {
                    UUID id = rs.getObject("user_id", UUID.class);
                    result.put(id, new UserSnapshot(id, rs.getString("username"),
                            rs.getString("full_name"), rs.getString("avatar_url")));
                });
        return result;
    }
}
