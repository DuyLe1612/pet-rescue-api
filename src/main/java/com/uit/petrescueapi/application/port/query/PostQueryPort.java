package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostFeedFilter;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PostQueryPort {
    /** @param viewerId null for a guest; then {@code likedByMe} is left out */
    PostResponseDto findById(UUID postId, UUID viewerId);
    Page<PostSummaryResponseDto> findAll(String search, PostFeedFilter filter, Pageable pageable);
    PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, UUID cursorId, int size, UUID viewerId, PostFeedFilter filter);
}
