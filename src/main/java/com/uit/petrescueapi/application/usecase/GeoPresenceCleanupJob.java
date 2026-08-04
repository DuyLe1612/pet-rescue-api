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

@Component
@RequiredArgsConstructor
@Slf4j
public class GeoPresenceCleanupJob {

    private final UserGeoLocationJpaRepository geoRepository;

    @Value("${app.geo.inactive-after-seconds:60}")
    private long inactiveAfterSeconds;

//    @Scheduled(fixedDelayString = "${app.geo.cleanup-interval-ms:10000}")
    @Transactional
    public void markStaleUsersInactive() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(inactiveAfterSeconds);
        geoRepository.markInactiveByLastSeenBefore(threshold);
    }
}
