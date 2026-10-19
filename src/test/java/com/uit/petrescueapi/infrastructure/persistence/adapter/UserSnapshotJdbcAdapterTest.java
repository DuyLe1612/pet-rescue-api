package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSnapshotJdbcAdapterTest {

    static final UUID A = UUID.randomUUID();
    static final UUID B = UUID.randomUUID();

    UserSnapshotJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(db);
        jdbc.getJdbcTemplate().execute("CREATE TABLE users (user_id UUID PRIMARY KEY, username VARCHAR(50), full_name VARCHAR(100), avatar_url VARCHAR(255))");
        jdbc.getJdbcTemplate().update("INSERT INTO users VALUES (?, 'alice', 'Alice Nguyen', 'a.png')", A);
        jdbc.getJdbcTemplate().update("INSERT INTO users VALUES (?, 'bob', NULL, NULL)", B);
        adapter = new UserSnapshotJdbcAdapter(jdbc);
    }

    @Test
    void loadsRequestedUsersInOneQuery() {
        Map<UUID, UserSnapshot> users = adapter.findByIds(List.of(A, B, UUID.randomUUID()));

        assertThat(users).containsOnlyKeys(A, B);
        assertThat(users.get(A)).isEqualTo(new UserSnapshot(A, "alice", "Alice Nguyen", "a.png"));
        assertThat(users.get(B).displayName()).isEqualTo("bob");
    }

    @Test
    void emptyInputSkipsQuery() {
        assertThat(adapter.findByIds(List.of())).isEmpty();
    }
}
