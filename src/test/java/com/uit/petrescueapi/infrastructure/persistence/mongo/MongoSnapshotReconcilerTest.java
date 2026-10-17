package com.uit.petrescueapi.infrastructure.persistence.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class MongoSnapshotReconcilerTest {

    @Test
    void resyncsOnlyUsersUpdatedInTheLast24Hours() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(db);
        jdbc.getJdbcTemplate().execute("CREATE TABLE users (user_id UUID PRIMARY KEY, updated_at TIMESTAMP)");
        UUID recent = UUID.randomUUID();
        UUID old = UUID.randomUUID();
        UUID never = UUID.randomUUID();
        jdbc.getJdbcTemplate().update("INSERT INTO users VALUES (?, ?)", recent, Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
        jdbc.getJdbcTemplate().update("INSERT INTO users VALUES (?, ?)", old, Timestamp.valueOf(LocalDateTime.now().minusDays(3)));
        jdbc.getJdbcTemplate().update("INSERT INTO users VALUES (?, NULL)", never);
        MongoSnapshotSyncListener listener = mock(MongoSnapshotSyncListener.class);

        new MongoSnapshotReconciler(jdbc, listener).reconcileRecentlyUpdatedUsers();

        verify(listener).resyncUser(recent);
        verifyNoMoreInteractions(listener);
    }
}
