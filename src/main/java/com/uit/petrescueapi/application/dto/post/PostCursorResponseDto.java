package com.uit.petrescueapi.application.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCursorResponseDto {
    private List<PostSummaryResponseDto> items;
    private LocalDateTime nextCursor;
    /** Tie-breaker for posts sharing {@code nextCursor}; send it back as {@code cursorId}. */
    private UUID nextCursorId;
    private boolean hasMore;
}
