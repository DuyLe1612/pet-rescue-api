package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.application.dto.adoption.AdoptionSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdoptionQueryPort {
    AdoptionResponseDto findById(UUID applicationId, UUID requesterId);
    Page<AdoptionSummaryResponseDto> findAll(List<String> statuses, String search, Pageable pageable, UUID requesterId);
    Page<AdoptionSummaryResponseDto> findByApplicantId(UUID applicantId, List<String> statuses, String search, Pageable pageable, UUID requesterId);
    Page<AdoptionSummaryResponseDto> findByOrganizationId(UUID organizationId, List<String> statuses, String search, Pageable pageable, UUID requesterId);

    /** Reclaim logs of one organization for its members; across all organizations for admins only. */
    void requireReclaimLogAccess(UUID organizationId, UUID requesterId);
}
