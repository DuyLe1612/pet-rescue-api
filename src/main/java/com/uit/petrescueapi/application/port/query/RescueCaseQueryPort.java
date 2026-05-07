package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseSummaryResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueMapMarkerDto;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RescueCaseQueryPort {
    RescueCaseResponseDto findById(UUID caseId);
    Page<RescueCaseSummaryResponseDto> findAll(Pageable pageable);
    Page<RescueCaseSummaryResponseDto> findNearby(double lat, double lng, double distanceMeters, Pageable pageable);
    Page<RescueCaseSummaryResponseDto> findWithinBoundingBox(double minLat, double minLng, double maxLat, double maxLng, Pageable pageable);

    // ── Map marker queries (optimized for fast map rendering) ──
    List<RescueMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng);
    List<RescueMapMarkerDto> findMarkersWithFilters(double minLat, double minLng,
                                                    double maxLat, double maxLng,
                                                    List<RescueCaseStatus> status,
                                                    List<RescuePriority> priority,
                                                    String species);
}
