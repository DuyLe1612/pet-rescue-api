package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.rescue.RescueQueueItemDto;
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

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescueCaseQueryAdapter implements RescueCaseQueryDataPort {

    /** The map is for finding animals that still need help; closed cases must be asked for. */
    private static final List<String> MAP_DEFAULT_STATUSES =
            List.of(RescueCaseStatus.REPORTED.name(), RescueCaseStatus.IN_PROGRESS.name());

    private final RescueCaseQueryJpaRepository queryRepo;
    private final CloudStoragePort cloudStoragePort;

    // ======================================================
    // LIST QUERIES
    // ======================================================

    @Override
    public Page<RescueCaseSummaryResponseDto> findAllSummaries(String search, RescueCaseFilter filter, Pageable pageable) {
        return queryRepo.findAllSummaries(search, allStatuses(filter), statusNames(filter), allPriorities(filter),
                        priorityNames(filter), filter.species(), filter.province(), pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public Page<RescueCaseCompletionResponseDto> findAllCompletion(boolean isResolved, Pageable pageable) {
        return withImages(queryRepo.findAllCompletion(isResolved, pageable));
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findNearbySummaries(
            double lat, double lng, double distanceMeters, RescueCaseFilter filter, Pageable pageable) {

        return queryRepo.findNearby(lng, lat, distanceMeters, allStatuses(filter), statusNames(filter),
                        allPriorities(filter), priorityNames(filter), filter.species(), filter.province(), pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findWithinBoundingBoxSummaries(
            double minLat, double minLng, double maxLat, double maxLng, RescueCaseFilter filter, Pageable pageable) {

        return queryRepo.findWithinBoundingBox(minLng, minLat, maxLng, maxLat, allStatuses(filter), statusNames(filter),
                        allPriorities(filter), priorityNames(filter), filter.species(), filter.province(), pageable)
                .map(this::toSummaryDto);
    }

    // ======================================================
    // DETAIL QUERY
    // ======================================================

    @Override
    public RescueCaseResponseDto findById(UUID caseId, UUID viewerId) {

        RescueCaseDetailProjection proj = queryRepo.findDetailById(caseId, viewerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("RescueCase", "caseId", caseId)
                );

        List<String> mediaUrls =
                buildMediaUrls(queryRepo.findMediaPublicIdsByCaseId(caseId));

        return toResponseDto(proj, mediaUrls, viewerId);
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

    // An empty IN () list is invalid SQL, so "no filter" travels as a flag plus a placeholder value.
    private static final List<String> NO_VALUES = List.of("");

    private static boolean allStatuses(RescueCaseFilter filter) {
        return filter.statuses() == null || filter.statuses().isEmpty();
    }

    private static List<String> statusNames(RescueCaseFilter filter) {
        return allStatuses(filter) ? NO_VALUES : filter.statuses().stream().map(Enum::name).toList();
    }

    private static boolean allPriorities(RescueCaseFilter filter) {
        return filter.priorities() == null || filter.priorities().isEmpty();
    }

    private static List<String> priorityNames(RescueCaseFilter filter) {
        return allPriorities(filter) ? NO_VALUES : filter.priorities().stream().map(Enum::name).toList();
    }

    private List<String> normalizeStatuses(List<RescueCaseStatus> status) {
        if (status == null || status.isEmpty()) {
            return MAP_DEFAULT_STATUSES;
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
                .reportedAt(p.getReportedAt())
                .locationText(p.getLocationText())
                .distanceMeters(p.getDistance())
                .firstImageUrl(p.getFirstImageUrl() == null ? null : cloudStoragePort.buildUrl(p.getFirstImageUrl()))
                .build();
    }

    private RescueCaseResponseDto toResponseDto(
            RescueCaseDetailProjection p,
            List<String> imageUrls,
            UUID viewerId) {

        return RescueCaseResponseDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .reporterUsername(p.getReporterUsername())
                .species(p.getSpecies())
                .color(p.getColor())
                .priority(p.getPriority() != null
                        ? RescuePriority.valueOf(p.getPriority())
                        : null)
                .description(p.getDescription())
                .status(p.getStatus())
                .latitude(p.getLocationLat())
                .longitude(p.getLocationLng())
                .reportedAt(p.getReportedAt())
                .imageUrls(imageUrls)
                .contactPhone(p.getContactPhone())
                .locationText(p.getLocationText())
                .wardName(p.getWardName())
                .provinceName(p.getProvinceName())
                .organizationId(p.getOrganizationId())
                .petId(p.getPetId())
                .needsHelp(Boolean.TRUE.equals(p.getNeedsHelp()))
                .activeClaimCount(p.getActiveClaimCount() == null ? 0 : p.getActiveClaimCount().intValue())
                .viewerIsReporter(viewerId != null && viewerId.equals(p.getReportedBy()))
                .viewerHasActiveClaim(Boolean.TRUE.equals(p.getViewerHasActiveClaim()))
                .completionId(p.getCompletionId())
                .completionStatus(p.getCompletionStatus())
                .build();
    }

    @Override
    public Page<RescueCaseCompletionResponseDto> findCompletionReviewQueue(Pageable pageable) {
        return withImages(queryRepo.findCompletionReviewQueue(pageable));
    }

    @Override
    public Page<RescueQueueItemDto> findOrganizationQueue(String provinceCode, Pageable pageable) {
        return queryRepo.findOrganizationQueue(provinceCode, pageable).map(p -> RescueQueueItemDto.builder()
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .species(p.getSpecies())
                .priority(p.getPriority() == null ? null : RescuePriority.valueOf(p.getPriority()))
                .status(p.getStatus())
                .description(p.getDescription())
                .provinceName(p.getProvinceName())
                .wardName(p.getWardName())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .reportedAt(p.getReportedAt())
                .needsHelp(p.getNeedsHelp())
                .awaitingHandover(p.getAwaitingHandover())
                .activeClaimCount(p.getActiveClaimCount())
                .build());
    }

    private Page<RescueCaseCompletionResponseDto> withImages(Page<RescueCaseCompletionProjection> page) {
        List<UUID> ids = page.getContent().stream().map(RescueCaseCompletionProjection::getCompletionId).toList();
        Map<UUID, List<String>> images = new HashMap<>();
        if (!ids.isEmpty()) {
            for (CompletionMediaProjection media : queryRepo.findMediaByCompletionIds(ids)) {
                images.computeIfAbsent(media.getCompletionId(), id -> new ArrayList<>())
                        .add(cloudStoragePort.buildUrl(media.getPublicId()));
            }
        }
        return page.map(p -> toCompletionDto(p, images.getOrDefault(p.getCompletionId(), List.of())));
    }

    private RescueCaseCompletionResponseDto toCompletionDto(
            RescueCaseCompletionProjection p,
            List<String> imageUrls) {

        return RescueCaseCompletionResponseDto.builder()
                .completionId(p.getCompletionId())
                .caseId(p.getCaseId())
                .caseCode(p.getCaseCode())
                .rescuedAt(p.getRescuedAt())
                .rescueNote(p.getRescueNote())
                .locationNote(p.getLocationNote())
                .verifiedBy(p.getVerifiedBy())
                .verifiedByName(p.getVerifiedByName())
                .verifiedAt(p.getVerifiedAt())
                .verificationImagesUrl(imageUrls)
                .submittedBy(p.getSubmittedBy())
                .submittedByName(p.getSubmittedByName())
                .outcome(p.getOutcome())
                .status(p.getStatus())
                .rejectReason(p.getRejectReason())
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