package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.application.dto.adoption.AdoptionSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.AdoptionQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.projection.AdoptionDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.AdoptionSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.AdoptionQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Query-side adapter (CQRS read path) for AdoptionApplication.
 *
 * <p>Executes optimized JOIN queries via {@link AdoptionQueryJpaRepository},
 * maps infrastructure projections to application DTOs.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdoptionQueryAdapter implements AdoptionQueryDataPort {

    private final AdoptionQueryJpaRepository queryRepo;
    private final CloudStoragePort cloudStoragePort;

    // ── List (summary) queries ──────────────────

    @Override
    public Page<AdoptionSummaryResponseDto> findAllSummaries(List<String> statuses, String search, Pageable pageable) {
        return queryRepo.findAllSummaries(normalizeStatuses(statuses), search, pageable).map(this::toSummaryDto);
    }

    @Override
    public Page<AdoptionSummaryResponseDto> findByApplicantIdSummaries(UUID applicantId, List<String> statuses, String search, Pageable pageable) {
        return queryRepo.findByApplicantIdSummaries(applicantId, normalizeStatuses(statuses), search, pageable).map(this::toSummaryDto);
    }

    // ── Detail (single adoption application) query ──

    @Override
    public AdoptionResponseDto findById(UUID applicationId) {
        AdoptionDetailProjection proj = queryRepo.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("AdoptionApplication", "applicationId", applicationId));
        return toResponseDto(proj);
    }

    // ── Projection → DTO mappers ────────────────

    private AdoptionSummaryResponseDto toSummaryDto(AdoptionSummaryProjection p) {
        return AdoptionSummaryResponseDto.builder()
                .applicationId(p.getApplicationId())
                .adoptionCode(p.getAdoptionCode())
                .petName(p.getPetName())
                .petPrimaryImageUrl(toImageUrl(p.getPetPrimaryImageUrl()))
                .applicantUsername(p.getApplicantUsername())
                .status(p.getStatus())
                .experience(p.getExperience())
                .liveCondition(p.getLiveCondition())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private AdoptionResponseDto toResponseDto(AdoptionDetailProjection p) {
        return AdoptionResponseDto.builder()
                .applicationId(p.getApplicationId())
                .adoptionCode(p.getAdoptionCode())
                .petId(p.getPetId())
                .petName(p.getPetName())
                .petPrimaryImageUrl(toImageUrl(p.getPetPrimaryImageUrl()))
                .applicantId(p.getApplicantId())
                .applicantUsername(p.getApplicantUsername())
                .organizationId(p.getOrganizationId())
                .organizationName(p.getOrganizationName())
                .status(p.getStatus())
            .note(p.getNote())
                .experience(p.getExperience())
                .liveCondition(p.getLiveCondition())
                .createdAt(p.getCreatedAt())
                .decidedAt(p.getDecidedAt())
                .decidedBy(p.getDecidedBy())
                .decidedByUsername(p.getDecidedByUsername())
                .rejectReason(p.getRejectReason())
                .readyAt(p.getReadyAt())
                .build();
    }

    private String toImageUrl(String publicId) {
        return publicId != null ? cloudStoragePort.buildUrl(publicId) : null;
    }

    private List<String> normalizeStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of("PENDING", "APPROVED", "REJECTED", "CANCELED", "COMPLETED");
        }
        List<String> normalized = new ArrayList<>();
        for (String status : statuses) {
            if (status == null || status.isBlank()) {
                continue;
            }
            normalized.add(status.trim().toUpperCase());
        }
        if (normalized.isEmpty()) {
            return List.of("PENDING", "APPROVED", "REJECTED", "CANCELED", "COMPLETED");
        }
        return normalized;
    }
}
