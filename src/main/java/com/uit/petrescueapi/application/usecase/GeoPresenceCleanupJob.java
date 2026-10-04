package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.infrastructure.persistence.repository.UserGeoLocationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Marks users in {@code user_geo_locations} as inactive once their last-seen
 * timestamp is older than {@code app.geo.inactive-after-seconds}. Without this
 * job the table grows forever (every active session writes a row, and the
 * app only overwrites a row on the next update, so disconnected sessions
 * stay marked active forever).
 *
 * <p>Backed by a single batched UPDATE — no rows are read into memory.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeoPresenceCleanupJob {

    private final UserGeoLocationJpaRepository geoRepository;

    @Value("${app.geo.inactive-after-seconds:60}")
    private long inactiveAfterSeconds;

    @Scheduled(
        fixedDelayString = "${app.geo.cleanup-interval-ms:30000}",
        initialDelayString = "${app.geo.cleanup-initial-delay-ms:5000}"
    )
    @Transactional
    public void markStaleUsersInactive() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(inactiveAfterSeconds);
        int updated = geoRepository.markInactiveByLastSeenBefore(threshold);
        if (updated > 0) {
            log.debug("Geo cleanup marked {} user(s) inactive", updated);
        }
    }
}