package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationCursorResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import java.util.UUID;

public interface ChatQueryPort {

    ConversationCursorResponseDto listConversationsByCursor(UUID userId, LocalDateTime cursor, int size);

    Page<ChatMessageDto> listMessages(UUID conversationId, UUID userId, Pageable pageable);
}
