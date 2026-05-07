package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.organization.OrganizationMemberResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationMapMarkerDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationSummaryResponseDto;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) port for Organization operations.
 * list → OrganizationSummaryResponseDto (lightweight, CQRS JOIN projection)
 * detail → OrganizationResponseDto
 * members → OrganizationMemberResponseDto (JOIN with users table)
 */
public interface OrganizationQueryPort {

    OrganizationResponseDto findById(UUID organizationId);

    Page<OrganizationSummaryResponseDto> findAll(List<OrganizationStatus> statuses, Pageable pageable);

    Page<OrganizationMemberResponseDto> findMembers(UUID organizationId, Pageable pageable);

    List<OrganizationMapMarkerDto> findMapMarkers(List<OrganizationStatus> statuses, List<String> types);

    Page<OrganizationSummaryResponseDto> findWithinBoundingBoxSummaries(double minLat, double minLng, double maxLat, double maxLng, List<OrganizationStatus> statuses, Pageable pageable);

    List<OrganizationMapMarkerDto> findMarkersInBounds(double minLat, double minLng, double maxLat, double maxLng, List<OrganizationStatus> statuses, List<String> types);
}
