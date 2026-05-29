package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.application.dto.adoption.AdoptionSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdoptionQueryPort {
    AdoptionResponseDto findById(UUID applicationId);
    Page<AdoptionSummaryResponseDto> findAll(List<String> statuses, String search, Pageable pageable);
    Page<AdoptionSummaryResponseDto> findByApplicantId(UUID applicantId, List<String> statuses, String search, Pageable pageable);
    Page<AdoptionSummaryResponseDto> findByOrganizationId(UUID organizationId, List<String> statuses, String search, Pageable pageable);
}
