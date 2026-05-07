package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseSummaryResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueMapMarkerDto;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Output port for RescueCase read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * JOIN queries and map projections directly to application DTOs.</p>
 */
public interface RescueCaseQueryDataPort {

    RescueCaseResponseDto findById(UUID caseId);

    Page<RescueCaseSummaryResponseDto> findAllSummaries(Pageable pageable);

    Page<RescueCaseSummaryResponseDto> findNearbySummaries(double lat, double lng, double distanceMeters, Pageable pageable);

    Page<RescueCaseSummaryResponseDto> findWithinBoundingBoxSummaries(double minLat, double minLng, double maxLat, double maxLng, Pageable pageable);

    // ── Map marker queries (ultra-lightweight, no pagination for fast rendering) ──

    /**
     * Find all active markers within bounding box - optimized for map display.
     * Returns minimal data for fast rendering.
     */
    List<RescueMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng);

    /**
     * Find markers with filters - for filtered map views.
     */
        List<RescueMapMarkerDto> findMarkersWithFilters(
            double minLat, double minLng, double maxLat, double maxLng,
            List<RescueCaseStatus> status, List<RescuePriority> priority, String species);

    /**
     * Find map markers without bounding box (global feed limited to 500). Filters optional.
     */
    List<RescueMapMarkerDto> findMapMarkers(List<RescueCaseStatus> status, List<RescuePriority> priority, String species);
}
