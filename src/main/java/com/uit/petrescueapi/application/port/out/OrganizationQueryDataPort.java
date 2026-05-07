package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.organization.OrganizationMemberResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationMapMarkerDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationSummaryResponseDto;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for the Organization CQRS read-side adapter.
 * Implemented by the infrastructure query adapter.
 */
public interface OrganizationQueryDataPort {
    Page<OrganizationSummaryResponseDto> findAllSummary(List<OrganizationStatus> statuses, Pageable pageable);
    Optional<OrganizationResponseDto> findById(UUID id);
    Page<OrganizationMemberResponseDto> findMembers(UUID organizationId, Pageable pageable);
    List<OrganizationMapMarkerDto> findMapMarkers(List<OrganizationStatus> statuses, List<String> types);
    
    Page<OrganizationSummaryResponseDto> findWithinBoundingBoxSummaries(double minLat, double minLng, double maxLat, double maxLng, List<OrganizationStatus> statuses, Pageable pageable);

    List<OrganizationMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng, List<OrganizationStatus> statuses, List<String> types);
}
