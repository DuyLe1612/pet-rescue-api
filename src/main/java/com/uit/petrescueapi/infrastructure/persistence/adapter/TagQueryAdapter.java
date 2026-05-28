package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.tag.TagSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.TagQueryDataPort;
import com.uit.petrescueapi.infrastructure.persistence.projection.TagSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.TagQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query-side adapter (CQRS read path) for Tag.
 *
 * <p>Executes optimized queries via {@link TagQueryJpaRepository},
 * maps infrastructure projections to application DTOs.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagQueryAdapter implements TagQueryDataPort {

    private final TagQueryJpaRepository queryRepo;

    // ── List (summary) queries ──────────────────

    @Override
    public Page<TagSummaryResponseDto> findAllSummaries(String search, Pageable pageable) {
        return queryRepo.findAllSummary(search, pageable).map(this::toSummaryDto);
    }

    // ── Projection → DTO mappers ────────────────

    private TagSummaryResponseDto toSummaryDto(TagSummaryProjection p) {
        return TagSummaryResponseDto.builder()
                .tagId(p.getTagId())
                .code(p.getCode())
                .name(p.getName())
                .build();
    }
}
