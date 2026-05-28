package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseSummaryResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueMapMarkerDto;
import com.uit.petrescueapi.application.port.out.RescueCaseQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueCaseDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueCaseSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RescueMapMarkerProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Query-side adapter (CQRS read path) for RescueCase.
 *
 * <p>Executes optimized JOIN queries via {@link RescueCaseQueryJpaRepository},
 * maps infrastructure projections to application DTOs.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescueCaseQueryAdapter implements RescueCaseQueryDataPort {

    private final RescueCaseQueryJpaRepository queryRepo;

    // ── List (summary) queries ──────────────────

    @Override
    public Page<RescueCaseSummaryResponseDto> findAllSummaries(String search, Pageable pageable) {
        return queryRepo.findAllSummaries(search, pageable).map(this::toSummaryDto);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findNearbySummaries(double lat, double lng,
                                                                   double distanceMeters,
                                                                   Pageable pageable) {
        // Port passes (lat, lng) but JPA repo expects (lng, lat) — swap parameters
        return queryRepo.findNearby(lng, lat, distanceMeters, pageable).map(this::toSummaryDto);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findWithinBoundingBoxSummaries(double minLat, double minLng,
                                                                             double maxLat, double maxLng,
                                                                             Pageable pageable) {
        // Port passes (minLat, minLng, maxLat, maxLng) but JPA repo expects (minLng, minLat, maxLng, maxLat)
        return queryRepo.findWithinBoundingBox(minLng, minLat, maxLng, maxLat, pageable).map(this::toSummaryDto);
    }

    // ── Detail (single rescue case) query ───────

    @Override
    public RescueCaseResponseDto findById(UUID caseId) {
        RescueCaseDetailProjection proj = queryRepo.findDetailById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("RescueCase", "caseId", caseId));
        return toResponseDto(proj, queryRepo.findMediaPublicIdsByCaseId(caseId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAP MARKER QUERIES - Ultra-optimized for fast map rendering
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<RescueMapMarkerDto> findMarkersInBounds(double minLat, double minLng,
                                                         double maxLat, double maxLng) {
        // Port passes (minLat, minLng, maxLat, maxLng) but repo expects (minLng, minLat, maxLng, maxLat)
        return queryRepo.findMarkersInBounds(minLng, minLat, maxLng, maxLat)
                .stream()
                .map(this::toMarkerDto)
                .toList();
    }

    @Override
    public List<RescueMapMarkerDto> findMarkersWithFilters(double minLat, double minLng,
                                                            double maxLat, double maxLng,
                                                            List<RescueCaseStatus> status,
                                                            List<RescuePriority> priority,
                                                            String species) {
        List<String> statusNames = (status == null || status.isEmpty())
            ? java.util.Arrays.stream(RescueCaseStatus.values()).map(Enum::name).toList()
            : status.stream().map(Enum::name).toList();
        List<String> priorityNames = (priority == null || priority.isEmpty())
            ? java.util.Arrays.stream(RescuePriority.values()).map(Enum::name).toList()
            : priority.stream().map(Enum::name).toList();

        return queryRepo.findMarkersWithFilters(minLng, minLat, maxLng, maxLat, statusNames, priorityNames, species)
                .stream()
                .map(this::toMarkerDto)
                .toList();
    }

    @Override
    public List<RescueMapMarkerDto> findMapMarkers(List<RescueCaseStatus> status, List<RescuePriority> priority, String species) {
        List<String> statusNames = (status == null || status.isEmpty())
            ? java.util.Arrays.stream(RescueCaseStatus.values()).map(Enum::name).toList()
            : status.stream().map(Enum::name).toList();
        List<String> priorityNames = (priority == null || priority.isEmpty())
            ? java.util.Arrays.stream(RescuePriority.values()).map(Enum::name).toList()
            : priority.stream().map(Enum::name).toList();

        return queryRepo.findMapMarkers(statusNames, priorityNames, species)
                .stream()
                .map(this::toMarkerDto)
                .toList();
    }

    // ── Projection → DTO mappers ────────────────

    private RescueCaseSummaryResponseDto toSummaryDto(RescueCaseSummaryProjection p) {
        return RescueCaseSummaryResponseDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .species(p.getSpecies())
                .priority(p.getPriority() != null ? RescuePriority.valueOf(p.getPriority()) : null)
                .status(p.getStatus())
                .reporterUsername(p.getReporterUsername())
                .locationText(p.getLocationText())
                .reportedAt(p.getReportedAt())
            .firstImageUrl(p.getFirstImageUrl())
                .build();
    }

    private RescueCaseResponseDto toResponseDto(RescueCaseDetailProjection p, java.util.List<String> imagePublicIds) {
        return RescueCaseResponseDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .petId(p.getPetId())
                .petName(p.getPetName())
                .reportedBy(p.getReportedBy())
                .reporterUsername(p.getReporterUsername())
                .organizationId(p.getOrganizationId())
                .organizationName(p.getOrganizationName())
                .species(p.getSpecies())
                .color(p.getColor())
                .size(p.getSize())
                .priority(p.getPriority() != null ? RescuePriority.valueOf(p.getPriority()) : null)
                .description(p.getDescription())
                .status(p.getStatus())
                .latitude(p.getLocationLat())
                .longitude(p.getLocationLng())
                .locationText(p.getLocationText())
                .wardName(p.getWardName())
                .provinceName(p.getProvinceName())
                .reportedAt(p.getReportedAt())
                .resolvedAt(p.getResolvedAt())
                .imageUrls(imagePublicIds)
                .contactPhone(p.getContactPhone())
                .build();
    }

    private RescueMapMarkerDto toMarkerDto(RescueMapMarkerProjection p) {
        return RescueMapMarkerDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .priority(p.getPriority() != null ? RescuePriority.valueOf(p.getPriority()) : null)
                .status(p.getStatus() != null ? RescueCaseStatus.valueOf(p.getStatus()) : null)
                .species(p.getSpecies())
                .reportedAt(p.getReportedAt())
                .build();
    }
}
