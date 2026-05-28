package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PostQueryPort {
    PostResponseDto findById(UUID postId);
    Page<PostSummaryResponseDto> findAll(String search, Pageable pageable);
    PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, int size, UUID viewerId);
}
