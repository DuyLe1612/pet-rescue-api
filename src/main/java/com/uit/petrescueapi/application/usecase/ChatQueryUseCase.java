package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationCursorResponseDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatQueryDataPort;
import com.uit.petrescueapi.application.port.query.ChatQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatQueryUseCase implements ChatQueryPort {

    private final ChatQueryDataPort chatQueryDataPort;

    @Override
    public ConversationCursorResponseDto listConversationsByCursor(UUID userId, LocalDateTime cursor, int size) {
        log.debug("Query: list conversations by cursor for user {}", userId);
        Page<ConversationSummaryDto> page = chatQueryDataPort.listConversationsByCursor(
                userId,
                cursor,
                org.springframework.data.domain.PageRequest.of(0, size)
        );
        var items = page.getContent();
        LocalDateTime nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getLastTime();
        boolean hasMore = items.size() == size;
        return ConversationCursorResponseDto.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Override
    public Page<ChatMessageDto> listMessages(UUID conversationId, UUID userId, Pageable pageable) {
        log.debug("Query: list messages for conversation {} (user {})", conversationId, userId);
        return chatQueryDataPort.listMessages(conversationId, userId, pageable);
    }
}
