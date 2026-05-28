package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ChatMessageCursorResponseDto;
import com.uit.petrescueapi.application.dto.chat.ConversationCursorResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import java.util.UUID;

public interface ChatQueryPort {

    ConversationCursorResponseDto listConversationsByCursor(UUID userId, LocalDateTime cursor, int size);

    ChatMessageCursorResponseDto listMessagesByCursor(UUID conversationId, UUID userId, LocalDateTime cursor, int size);
}
