package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.organization.OrganizationMemberResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationMapMarkerDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.OrganizationQueryDataPort;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationMemberProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationMapMarkerProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.OrganizationMemberJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.OrganizationQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationQueryAdapter implements OrganizationQueryDataPort {

    private final OrganizationQueryJpaRepository queryRepo;
    private final OrganizationMemberJpaRepository memberRepo;

    @Override
    public Page<OrganizationSummaryResponseDto> findAllSummary(List<OrganizationStatus> statuses, Pageable pageable) {
        List<OrganizationStatus> normalizedStatuses = (statuses == null || statuses.isEmpty())
                ? Arrays.stream(OrganizationStatus.values()).toList()
                : statuses;
        return queryRepo.findAllSummary(normalizedStatuses, pageable).map(this::toSummaryDto);
    }

    @Override
    public Optional<OrganizationResponseDto> findById(UUID id) {
        return queryRepo.findDetailById(id).map(this::toDetailDto);
    }

    @Override
    public Page<OrganizationMemberResponseDto> findMembers(UUID organizationId, Pageable pageable) {
        return memberRepo.findByOrganizationId(organizationId, pageable).map(this::toMemberDto);
    }

    @Override
    public List<OrganizationMapMarkerDto> findMapMarkers(List<OrganizationStatus> statuses, List<String> types) {
        List<OrganizationStatus> normalizedStatuses = (statuses == null || statuses.isEmpty())
                ? Arrays.stream(OrganizationStatus.values()).toList()
                : statuses;
        List<String> statusNames = normalizedStatuses.stream().map(Enum::name).toList();
        List<String> normalizedTypes = (types == null || types.isEmpty())
            ? List.of("SHELTER", "VET_CENTER")
            : types;
        return queryRepo.findMapMarkers(statusNames, normalizedTypes).stream().map(this::toMarkerDto).toList();
    }

    // ── Private mapping helpers ──────────────────────────

    private OrganizationSummaryResponseDto toSummaryDto(OrganizationSummaryProjection p) {
        return OrganizationSummaryResponseDto.builder()
                .organizationId(p.getOrganizationId())
                .organizationCode(p.getOrganizationCode())
                .name(p.getName())
                .type(p.getType())
                .status(p.getStatus())
                .streetAddress(p.getStreetAddress())
                .wardName(p.getWardName())
                .provinceName(p.getProvinceName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .imageUrl(p.getImageUrl())
                .build();
    }

    private OrganizationResponseDto toDetailDto(OrganizationDetailProjection p) {
        return OrganizationResponseDto.builder()
                .organizationId(p.getOrganizationId())
                .organizationCode(p.getOrganizationCode())
                .name(p.getName())
                .description(p.getDescription())
                .type(p.getType())
                .streetAddress(p.getStreetAddress())
                .wardName(p.getWardName())
                .provinceName(p.getProvinceName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .imageUrl(p.getImageUrl())
                .officialLink(p.getOfficialLink())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .status(p.getStatus())
                .requestedByUserId(p.getRequestedByUserId())
                .requestedByUsername(p.getRequestedByUsername())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private OrganizationMemberResponseDto toMemberDto(OrganizationMemberProjection p) {
        return OrganizationMemberResponseDto.builder()
                .organizationId(p.getOrganizationId())
                .organizationName(p.getOrganizationName())
                .userId(p.getUserId())
                .username(p.getUsername())
                .role(p.getRole())
                .status(p.getStatus())
                .joinedAt(p.getJoinedAt())
                .build();
    }

    private OrganizationMapMarkerDto toMarkerDto(OrganizationMapMarkerProjection p) {
        return OrganizationMapMarkerDto.builder()
                .organizationId(p.getOrganizationId())
                .organizationCode(p.getOrganizationCode())
                .name(p.getName())
                .type(p.getType())
                .status(p.getStatus())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .phone(p.getPhone())
                .imageUrl(p.getImageUrl())
                .wardName(p.getWardName())
                .provinceName(p.getProvinceName())
                .build();
    }
}
