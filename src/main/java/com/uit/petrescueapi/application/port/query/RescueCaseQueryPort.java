package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.rescue.RescueClaimResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueQueueItemDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseCompletionResponseDto;
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
    /** {@code viewerId} is null for an anonymous caller. */
    RescueCaseResponseDto findById(UUID caseId, UUID viewerId);
    Page<RescueCaseSummaryResponseDto> findAll(String search, Pageable pageable);
    Page<RescueCaseSummaryResponseDto> findNearby(double lat, double lng, double distanceMeters, Pageable pageable);
    Page<RescueCaseSummaryResponseDto> findWithinBoundingBox(double minLat, double minLng, double maxLat, double maxLng, Pageable pageable);

    // ── Map marker queries (optimized for fast map rendering) ──
    List<RescueMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng);
    List<RescueMapMarkerDto> findMarkersWithFilters(double minLat, double minLng,
                                                    double maxLat, double maxLng,
                                                    List<RescueCaseStatus> status,
                                                    List<RescuePriority> priority,
                                                    String species);

    /**
     * Global map feed (no bounding box). Filters optional; limited to 500 markers.
     */
    List<RescueMapMarkerDto> findMapMarkers(List<RescueCaseStatus> status, List<RescuePriority> priority, String species);

    RescueCaseCompletionResponseDto getCompletionDetails(UUID caseId);
    Page<RescueCaseCompletionResponseDto> findAllCompletions(boolean isResolved,Pageable pageable);

    Page<RescueCaseCompletionResponseDto> findCompletionReviewQueue(Pageable pageable);

    /** Admins see every province; members see their organization's province. */
    Page<RescueQueueItemDto> findOrganizationQueue(UUID requesterId, Pageable pageable);

    List<RescueClaimResponseDto> findClaimsOfCase(UUID caseId, UUID requesterId);

    List<RescueClaimResponseDto> findMyActiveClaims(UUID userId);
}

