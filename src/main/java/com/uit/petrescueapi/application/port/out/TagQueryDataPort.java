package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.tag.TagSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Output port for Tag read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * queries and map projections directly to application DTOs.</p>
 */
public interface TagQueryDataPort {

    Page<TagSummaryResponseDto> findAllSummaries(String search, Pageable pageable);
}
