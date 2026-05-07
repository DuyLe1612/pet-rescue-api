package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.organization.OrganizationMemberResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationMapMarkerDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.OrganizationQueryDataPort;
import com.uit.petrescueapi.application.port.query.OrganizationQueryPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) use-case for Organization operations.
 *
 * <p>Thin orchestrator — delegates directly to {@link OrganizationQueryDataPort}
 * (infrastructure query adapter). Queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationQueryUseCase implements OrganizationQueryPort {

    private final OrganizationQueryDataPort queryDataPort;

    @Override
    public OrganizationResponseDto findById(UUID organizationId) {
        return queryDataPort.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
    }

    @Override
    public Page<OrganizationSummaryResponseDto> findAll(List<OrganizationStatus> statuses, Pageable pageable) {
        return queryDataPort.findAllSummary(statuses, pageable);
    }

    @Override
    public Page<OrganizationMemberResponseDto> findMembers(UUID organizationId, Pageable pageable) {
        return queryDataPort.findMembers(organizationId, pageable);
    }

    @Override
    public List<OrganizationMapMarkerDto> findMapMarkers(List<OrganizationStatus> statuses, List<String> types) {
        return queryDataPort.findMapMarkers(statuses, types);
    }

    public Page<OrganizationSummaryResponseDto> findWithinBoundingBoxSummaries(double minLat, double minLng, double maxLat, double maxLng, List<OrganizationStatus> statuses, Pageable pageable) {
        return queryDataPort.findWithinBoundingBoxSummaries(minLat, minLng, maxLat, maxLng, statuses, pageable);
    }

    public List<OrganizationMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng, List<OrganizationStatus> statuses, List<String> types) {
        return queryDataPort.findMarkersInBounds(minLat, minLng, maxLat, maxLng, statuses, types);
    }
}
