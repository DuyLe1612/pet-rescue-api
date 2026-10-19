package com.uit.petrescueapi.infrastructure.persistence.mongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Safety net for {@link MongoSnapshotSyncListener}: an in-process event is lost if the app
 * stops between the PostgreSQL commit and the MongoDB update. Once a night, users changed in the
 * last 24 h are re-synced (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.storage.chat:postgres}' == 'mongo' or '${app.storage.post:postgres}' == 'mongo'")
public class MongoSnapshotReconciler {

    private final NamedParameterJdbcTemplate jdbc;
    private final MongoSnapshotSyncListener sync;

    @Scheduled(cron = "${app.mongo.snapshot-reconcile-cron:0 30 3 * * *}")
    public void reconcileRecentlyUpdatedUsers() {
        List<UUID> ids = jdbc.queryForList("SELECT user_id FROM users WHERE updated_at >= :since",
                Map.of("since", Timestamp.valueOf(LocalDateTime.now().minusHours(24))), UUID.class);
        ids.forEach(sync::resyncUser);
        log.info("Snapshot reconcile: re-synced {} recently updated users", ids.size());
    }
}
