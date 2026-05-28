package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Output port for Post read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * JOIN queries and map projections directly to application DTOs.</p>
 */
public interface PostQueryDataPort {

    PostResponseDto findById(UUID postId);

    Page<PostSummaryResponseDto> findAllSummaries(String search, Pageable pageable);

    PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, int size, UUID viewerId);
}
