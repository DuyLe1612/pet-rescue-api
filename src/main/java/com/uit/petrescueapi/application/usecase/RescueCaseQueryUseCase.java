package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseFilter;
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

    private final RescueCaseQueryDataPort queryDataPort;
    private final RescueCaseDomainService caseDomainService;
    private final RescueClaimDomainService claimDomainService;
    private final OrganizationDomainService organizationDomainService;
    private final UserDomainService userDomainService;
    private final OrganizationMemberRepository memberRepository;
    private final AccessControlDomainService accessControl;
    @Override
    public RescueCaseResponseDto findById(UUID caseId, UUID viewerId) {
        log.debug("Query: find rescue case by id {}", caseId);
        RescueCaseResponseDto dto = queryDataPort.findById(caseId, viewerId);
        if (!accessControl.canViewCaseContact(dto.isViewerIsReporter(), dto.getOrganizationId(), dto.isViewerHasActiveClaim(), viewerId)) {
            dto.setContactPhone(null);
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
    public Page<RescueCaseSummaryResponseDto> findWithinBoundingBox(double minLat, double minLng, double maxLat, double maxLng, RescueCaseFilter filter, Pageable pageable) {
        log.debug("Query: find rescue cases within bounding box ({},{}) to ({},{})", minLat, minLng, maxLat, maxLng);
        return queryDataPort.findWithinBoundingBoxSummaries(minLat, minLng, maxLat, maxLng, orNone(filter), pageable);
    }

    private static RescueCaseFilter orNone(RescueCaseFilter filter) {
        return filter == null ? RescueCaseFilter.none() : filter;
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
            dto.setUsername(userDomainService.findById(claim.getUserId()).getUsername());
            return dto;
        }).toList();
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
