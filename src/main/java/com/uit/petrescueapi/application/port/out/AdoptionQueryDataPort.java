package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.application.dto.adoption.AdoptionSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Output port for Adoption read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * JOIN queries and map projections directly to application DTOs.</p>
 */
public interface AdoptionQueryDataPort {

    AdoptionResponseDto findById(UUID applicationId);

    Page<AdoptionSummaryResponseDto> findAllSummaries(List<String> statuses, String search, Pageable pageable);

    Page<AdoptionSummaryResponseDto> findByApplicantIdSummaries(UUID applicantId, List<String> statuses, String search, Pageable pageable);
}
