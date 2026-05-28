package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.tag.TagSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TagQueryPort {
    Page<TagSummaryResponseDto> findAll(String search, Pageable pageable);
}
