package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.tag.TagSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.TagQueryDataPort;
import com.uit.petrescueapi.application.port.query.TagQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Query (read) use-case for Tag operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link TagQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagQueryUseCase implements TagQueryPort {

    private final TagQueryDataPort queryDataPort;

    @Override
    public Page<TagSummaryResponseDto> findAll(String search, Pageable pageable) {
        log.debug("Query: find all tags (paginated, search={})", search);
        return queryDataPort.findAllSummaries(search, pageable);
    }
}
