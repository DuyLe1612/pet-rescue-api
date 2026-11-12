package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostFeedFilter;
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

    boolean isLikedBy(UUID postId, UUID userId);

    default Page<PostSummaryResponseDto> findAllSummaries(String search, Pageable pageable) {
        return findAllSummaries(search, PostFeedFilter.none(), pageable);
    }

    Page<PostSummaryResponseDto> findAllSummaries(String search, PostFeedFilter filter, Pageable pageable);

    /**
     * Feed page ordered by {@code (createdAt, postId)} descending. Pass the previous page's
     * {@code nextCursor} + {@code nextCursorId}; {@code cursorId} may be null for old clients.
     */
    /** Whether this author already has a live post about the case, other than {@code exceptPostId} (may be null). */
    boolean existsByAuthorAndRescueCase(UUID authorId, UUID caseId, UUID exceptPostId);

    PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, UUID cursorId, int size, UUID viewerId, PostFeedFilter filter);
}
