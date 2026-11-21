package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseFilter;
import com.uit.petrescueapi.application.service.SpeciesResolver;
import com.uit.petrescueapi.application.dto.rescue.RescueClaimResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueQueueItemDto;
import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.repository.OrganizationMemberRepository;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import com.uit.petrescueapi.domain.service.RescueClaimDomainService;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseCompletionResponseDto;
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

    /** Estimates, not measured: a street corner, and long enough to cover a second passer-by the next day. */
    private static final double DUPLICATE_RADIUS_METERS = 300;
    private static final double DUPLICATE_MAX_RADIUS_METERS = 2000;
    private static final int DUPLICATE_WINDOW_HOURS = 48;
    private static final int DUPLICATE_MAX_WINDOW_HOURS = 168;
    private static final int DUPLICATE_MAX_RESULTS = 5;

    private final RescueCaseQueryDataPort queryDataPort;
    private final RescueCaseDomainService caseDomainService;
    private final RescueClaimDomainService claimDomainService;
    private final OrganizationDomainService organizationDomainService;
    private final UserDomainService userDomainService;
    private final OrganizationMemberRepository memberRepository;
    private final AccessControlDomainService accessControl;
    private final com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository triageRepository;
    private final SpeciesResolver speciesResolver;
    @Override
    public RescueCaseResponseDto findById(UUID caseId, UUID viewerId) {
        log.debug("Query: find rescue case by id {}", caseId);
        RescueCaseResponseDto dto = queryDataPort.findById(caseId, viewerId);
        boolean insider = accessControl.canViewCaseContact(dto.isViewerIsReporter(), dto.getOrganizationId(), dto.isViewerHasActiveClaim(), viewerId);
        if (!insider) {
            dto.setContactPhone(null);
        }
        if (dto.getLocationBlurMeters() != null) {
            if (insider) {
                dto.setLocationBlurMeters(null);     // the exact point, for the people working on it
            } else {
                dto.setLatitude(dto.getBlurredLatitude());
                dto.setLongitude(dto.getBlurredLongitude());
                dto.setLocationText(null);
            }
        }
        return dto;
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findAll(String search, RescueCaseFilter filter, Pageable pageable) {
        log.debug("Query: find all rescue cases (paginated, search={})", search);
        return queryDataPort.findAllSummaries(search, orNone(filter), pageable);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findNearby(double lat, double lng, double distanceMeters, RescueCaseFilter filter, Pageable pageable) {
        log.debug("Query: find rescue cases nearby ({}, {}) within {} meters", lat, lng, distanceMeters);
        return queryDataPort.findNearbySummaries(lat, lng, distanceMeters, orNone(filter), pageable);
    }

    @Override
    public List<RescueCaseSummaryResponseDto> findPossibleDuplicates(double lat, double lng, String species, Double radiusMeters, Integer hours) {
        double radius = radiusMeters == null ? DUPLICATE_RADIUS_METERS : Math.clamp(radiusMeters, 1, DUPLICATE_MAX_RADIUS_METERS);
        int window = hours == null ? DUPLICATE_WINDOW_HOURS : Math.clamp(hours, 1, DUPLICATE_MAX_WINDOW_HOURS);
        String wanted = speciesResolver.canonicalForFilter(species);
        log.debug("Query: possible duplicate rescue cases near ({}, {}) within {} m, {} h", lat, lng, radius, window);
        return queryDataPort.findPossibleDuplicates(lat, lng, radius, wanted, window, DUPLICATE_MAX_RESULTS);
    }

    @Override
    public Page<RescueCaseSummaryResponseDto> findWithinBoundingBox(double minLat, double minLng, double maxLat, double maxLng, RescueCaseFilter filter, Pageable pageable) {
        log.debug("Query: find rescue cases within bounding box ({},{}) to ({},{})", minLat, minLng, maxLat, maxLng);
        return queryDataPort.findWithinBoundingBoxSummaries(minLat, minLng, maxLat, maxLng, orNone(filter), pageable);
    }

    /** A filter on "chó" or "DOG" looks for the stored "Dog". */
    private RescueCaseFilter orNone(RescueCaseFilter filter) {
        if (filter == null) {
            return RescueCaseFilter.none();
        }
        return new RescueCaseFilter(filter.statuses(), filter.priorities(),
                speciesResolver.canonicalForFilter(filter.species()), filter.province(), filter.reporterId(), filter.types());
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
                                                           List<RescueCaseStatus> status, List<RescuePriority> priority, String species, List<com.uit.petrescueapi.domain.valueobject.RescueCaseType> types) {
        log.debug("Query: find map markers with filters - bounds ({},{}) to ({},{}), status={}, priority={}, species={}",
                minLat, minLng, maxLat, maxLng, status, priority, species);
        return queryDataPort.findMarkersWithFilters(minLat, minLng, maxLat, maxLng, status, priority,
                speciesResolver.canonicalForFilter(species), types);
    }

    public List<RescueMapMarkerDto> findMapMarkers(List<RescueCaseStatus> status, List<RescuePriority> priority, String species, List<com.uit.petrescueapi.domain.valueobject.RescueCaseType> types) {
        log.debug("Query: find global map markers (no bbox), status={}, priority={}, species={}", status, priority, species);
        return queryDataPort.findMapMarkers(status, priority, speciesResolver.canonicalForFilter(species), types);
    }

    @Override
    public RescueCaseCompletionResponseDto getCompletionDetails(UUID completionId) {
        log.debug("Query: get completion details for rescue case completions {}", completionId);
        return queryDataPort.findCompletionById(completionId);
    }

    @Override
    public Page<RescueCaseCompletionResponseDto> findAllCompletions(boolean isResolved,Pageable pageable) {
        log.debug("Query: find all completions");
        return queryDataPort.findAllCompletion(isResolved,pageable);
    }

    @Override
    public Page<RescueCaseCompletionResponseDto> findCompletionReviewQueue(Pageable pageable) {
        return queryDataPort.findCompletionReviewQueue(pageable);
    }

    @Override
    public Page<RescueQueueItemDto> findOrganizationQueue(UUID requesterId, Pageable pageable) {
        if (accessControl.isAdmin(requesterId)) {
            return queryDataPort.findOrganizationQueue(null, pageable);
        }
        UUID organizationId = memberRepository.findOrganizationIdByUserId(requesterId);
        accessControl.requireOrganizationHandler(organizationId, requesterId);
        return queryDataPort.findOrganizationQueue(organizationDomainService.findById(organizationId).getProvinceCode(), pageable);
    }

    @Override
    public List<RescueClaimResponseDto> findClaimsOfCase(UUID caseId, UUID requesterId) {
        accessControl.requireCaseEditor(caseDomainService.findById(caseId), requesterId);
        return claimDomainService.findActiveByCase(caseId).stream().map(claim -> {
            RescueClaimResponseDto dto = RescueClaimResponseDto.from(claim);
            dto.setName(com.uit.petrescueapi.application.support.DisplayName.of(
                    userDomainService.findById(claim.getUserId()).getFullName()));
            return dto;
        }).toList();
    }

    @Override
    public com.uit.petrescueapi.application.dto.rescue.RescueTriageResponseDto findTriage(UUID caseId, UUID requesterId) {
        var rescueCase = caseDomainService.findById(caseId);
        accessControl.requireCaseHandler(rescueCase, requesterId);
        return triageRepository.findByCaseId(caseId)
                .map(t -> com.uit.petrescueapi.application.dto.rescue.RescueTriageResponseDto.from(t, rescueCase.getPriority()))
                .orElseGet(() -> com.uit.petrescueapi.application.dto.rescue.RescueTriageResponseDto.notStarted(caseId, rescueCase.getPriority()));
    }

    @Override
    public List<RescueClaimResponseDto> findMyActiveClaims(UUID userId) {
        return claimDomainService.findActiveByUser(userId).stream().map(claim -> {
            RescueClaimResponseDto dto = RescueClaimResponseDto.from(claim);
            dto.setCaseCode(caseDomainService.findById(claim.getCaseId()).getCaseCode());
            return dto;
        }).toList();
    }
}
