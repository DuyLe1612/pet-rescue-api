package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseSummaryResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueMapMarkerDto;
import com.uit.petrescueapi.application.port.out.RescueCaseQueryDataPort;
import com.uit.petrescueapi.application.port.query.RescueCaseQueryPort;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) use-case for RescueCase operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link RescueCaseQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RescueCaseQueryUseCase implements RescueCaseQueryPort {

    private final RescueCaseQueryDataPort queryDataPort;

    @Override
    public RescueCaseResponseDto findById(UUID caseId) {
        log.debug("Query: find rescue case by id {}", caseId);
        return queryDataPort.findById(caseId);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findAll(String search, Pageable pageable) {
        log.debug("Query: find all rescue cases (paginated, search={})", search);
        return queryDataPort.findAllSummaries(search, pageable);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findNearby(double lat, double lng, double distanceMeters, Pageable pageable) {
        log.debug("Query: find rescue cases nearby ({}, {}) within {} meters", lat, lng, distanceMeters);
        return queryDataPort.findNearbySummaries(lat, lng, distanceMeters, pageable);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findWithinBoundingBox(double minLat, double minLng, double maxLat, double maxLng, Pageable pageable) {
        log.debug("Query: find rescue cases within bounding box ({},{}) to ({},{})", minLat, minLng, maxLat, maxLng);
        return queryDataPort.findWithinBoundingBoxSummaries(minLat, minLng, maxLat, maxLng, pageable);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAP MARKER QUERIES - Optimized for fast map rendering
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<RescueMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng) {
        log.debug("Query: find map markers in bounds ({},{}) to ({},{})", minLat, minLng, maxLat, maxLng);
        return queryDataPort.findMarkersInBounds(minLat, minLng, maxLat, maxLng);
    }

    @Override
    public List<RescueMapMarkerDto> findMarkersWithFilters(double minLat, double minLng, double maxLat, double maxLng,
                                                           List<RescueCaseStatus> status, List<RescuePriority> priority, String species) {
        log.debug("Query: find map markers with filters - bounds ({},{}) to ({},{}), status={}, priority={}, species={}",
                minLat, minLng, maxLat, maxLng, status, priority, species);
        return queryDataPort.findMarkersWithFilters(minLat, minLng, maxLat, maxLng, status, priority, species);
    }

    public List<RescueMapMarkerDto> findMapMarkers(List<RescueCaseStatus> status, List<RescuePriority> priority, String species) {
        log.debug("Query: find global map markers (no bbox), status={}, priority={}, species={}", status, priority, species);
        return queryDataPort.findMapMarkers(status, priority, species);
    }
}
