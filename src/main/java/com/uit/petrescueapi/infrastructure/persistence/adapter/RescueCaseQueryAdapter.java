package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.rescue.*;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.RescueCaseQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.infrastructure.persistence.projection.*;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescueCaseQueryAdapter implements RescueCaseQueryDataPort {

    private final RescueCaseQueryJpaRepository queryRepo;
    private final CloudStoragePort cloudStoragePort;

    // ======================================================
    // LIST QUERIES
    // ======================================================

    @Override
    public Page<RescueCaseSummaryResponseDto> findAllSummaries(String search, Pageable pageable) {
        return queryRepo.findAllSummaries(search, pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public Page<RescueCaseCompletionResponseDto> findAllCompletion(boolean isResolved, Pageable pageable) {
        return queryRepo.findAllCompletion(isResolved, pageable)
                .map(p -> toCompletionDto(
                        p,
                        buildMediaUrls(queryRepo.findMediaPublicIdsByCompletionId(p.getCompletionId()))
                ));
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findNearbySummaries(
            double lat, double lng, double distanceMeters, Pageable pageable) {

        return queryRepo.findNearby(lng, lat, distanceMeters, pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findWithinBoundingBoxSummaries(
            double minLat, double minLng, double maxLat, double maxLng, Pageable pageable) {

        return queryRepo.findWithinBoundingBox(minLng, minLat, maxLng, maxLat, pageable)
                .map(this::toSummaryDto);
    }

    // ======================================================
    // DETAIL QUERY
    // ======================================================

    @Override
    public RescueCaseResponseDto findById(UUID caseId) {

        RescueCaseDetailProjection proj = queryRepo.findDetailById(caseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("RescueCase", "caseId", caseId)
                );

        List<String> mediaUrls =
                buildMediaUrls(queryRepo.findMediaPublicIdsByCaseId(caseId));

        return toResponseDto(proj, mediaUrls);
    }

    @Override
    public RescueCaseCompletionResponseDto findCompletionById(UUID completionId) {

        RescueCaseCompletionProjection proj = queryRepo.findCompletionById(completionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("RescueCaseCompletion", "completionId", completionId)
                );

        List<String> mediaUrls =
                buildMediaUrls(queryRepo.findMediaPublicIdsByCompletionId(completionId));

        return toCompletionDto(proj, mediaUrls);
    }

    // ======================================================
    // MAP MARKERS
    // ======================================================

    @Override
    public List<RescueMapMarkerDto> findMarkersInBounds(
            double minLat, double minLng, double maxLat, double maxLng) {

        return queryRepo.findMarkersInBounds(minLng, minLat, maxLng, maxLat)
                .stream()
                .map(this::toMarkerDto)
                .toList();
    }

    @Override
    public List<RescueMapMarkerDto> findMarkersWithFilters(
            double minLat, double minLng,
            double maxLat, double maxLng,
            List<RescueCaseStatus> status,
            List<RescuePriority> priority,
            String species) {

        List<String> statusNames = normalizeStatuses(status);
        List<String> priorityNames = normalizePriorities(priority);

        return queryRepo.findMarkersWithFilters(
                        minLng, minLat, maxLng, maxLat,
                        statusNames, priorityNames, species)
                .stream()
                .map(this::toMarkerDto)
                .toList();
    }

    @Override
    public List<RescueMapMarkerDto> findMapMarkers(
            List<RescueCaseStatus> status,
            List<RescuePriority> priority,
            String species) {

        return queryRepo.findMapMarkers(
                        normalizeStatuses(status),
                        normalizePriorities(priority),
                        species)
                .stream()
                .map(this::toMarkerDto)
                .toList();
    }

    // ======================================================
    // COMMON HELPERS
    // ======================================================

    private List<String> buildMediaUrls(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return List.of();
        }

        return publicIds.stream()
                .filter(Objects::nonNull)
                .map(cloudStoragePort::buildUrl)
                .toList();
    }

    private List<String> normalizeStatuses(List<RescueCaseStatus> status) {
        if (status == null || status.isEmpty()) {
            return Arrays.stream(RescueCaseStatus.values())
                    .map(Enum::name)
                    .toList();
        }
        return status.stream().map(Enum::name).toList();
    }

    private List<String> normalizePriorities(List<RescuePriority> priority) {
        if (priority == null || priority.isEmpty()) {
            return Arrays.stream(RescuePriority.values())
                    .map(Enum::name)
                    .toList();
        }
        return priority.stream().map(Enum::name).toList();
    }

    // ======================================================
    // MAPPERS
    // ======================================================

    private RescueCaseSummaryResponseDto toSummaryDto(RescueCaseSummaryProjection p) {
        return RescueCaseSummaryResponseDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .species(p.getSpecies())
                .priority(p.getPriority() != null
                        ? RescuePriority.valueOf(p.getPriority())
                        : null)
                .status(p.getStatus())
                .reporterUsername(p.getReporterUsername())
                .locationText(p.getLocationText())
                .reportedAt(p.getReportedAt())
                .firstImageUrl(p.getFirstImageUrl())
                .build();
    }

    private RescueCaseResponseDto toResponseDto(
            RescueCaseDetailProjection p,
            List<String> imageUrls) {

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
                .priority(p.getPriority() != null
                        ? RescuePriority.valueOf(p.getPriority())
                        : null)
                .description(p.getDescription())
                .status(p.getStatus())
                .latitude(p.getLocationLat())
                .longitude(p.getLocationLng())
                .locationText(p.getLocationText())
                .wardName(p.getWardName())
                .provinceName(p.getProvinceName())
                .reportedAt(p.getReportedAt())
                .resolvedAt(p.getResolvedAt())
                .imageUrls(imageUrls)
                .contactPhone(p.getContactPhone())
                .build();
    }

    private RescueCaseCompletionResponseDto toCompletionDto(
            RescueCaseCompletionProjection p,
            List<String> imageUrls) {

        return RescueCaseCompletionResponseDto.builder()
                .completionId(p.getCompletionId())
                .caseId(p.getCaseId())
                .rescuedAt(p.getRescuedAt())
                .rescueNote(p.getRescueNote())
                .locationNote(p.getLocationNote())
                .verifiedBy(p.getVerifiedBy())
                .verifiedByName(p.getVerifiedByName())
                .verifiedAt(p.getVerifiedAt())
                .verificationImagesUrl(imageUrls)
                .build();
    }

    private RescueMapMarkerDto toMarkerDto(RescueMapMarkerProjection p) {
        return RescueMapMarkerDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .priority(p.getPriority() != null
                        ? RescuePriority.valueOf(p.getPriority())
                        : null)
                .status(p.getStatus() != null
                        ? RescueCaseStatus.valueOf(p.getStatus())
                        : null)
                .species(p.getSpecies())
                .reportedAt(p.getReportedAt())
                .build();
    }
}