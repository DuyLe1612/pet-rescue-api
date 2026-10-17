package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.chat.ChatMessageCursorResponseDto;
import com.uit.petrescueapi.application.dto.chat.ConversationCursorResponseDto;

import java.time.LocalDateTime;

import java.util.UUID;

public interface ChatQueryPort {

    ConversationCursorResponseDto listConversationsByCursor(UUID userId, LocalDateTime cursor, int size);

    ChatMessageCursorResponseDto listMessagesByCursor(UUID conversationId, UUID userId, LocalDateTime cursor, Long cursorSeq, int size, String direction);
}
