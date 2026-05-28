package com.uit.petrescueapi.application.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCursorResponseDto {
    private List<ChatMessageDto> items;
    private LocalDateTime nextCursor;
    private boolean hasMore;
}
