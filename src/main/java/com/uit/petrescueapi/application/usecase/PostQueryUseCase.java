package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.application.port.query.PostQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Query (read) use-case for Post operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link PostQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostQueryUseCase implements PostQueryPort {

    private final PostQueryDataPort queryDataPort;

    @Override
    public PostResponseDto findById(UUID postId) {
        log.debug("Query: find post by id {}", postId);
        return queryDataPort.findById(postId);
    }

    @Override
    public Page<PostSummaryResponseDto> findAll(String search, Pageable pageable) {
        log.debug("Query: find all posts (paginated, search={})", search);
        return queryDataPort.findAllSummaries(search, pageable);
    }

    @Override
    public PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, int size, UUID viewerId) {
        log.debug("Query: find post feed by cursor {}, size {}", cursor, size);
        return queryDataPort.findFeedByCursor(cursor, size, viewerId);
    }
}
