package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ChatQueryDataPort {

    Page<ConversationSummaryDto> listConversationsByCursor(UUID userId, LocalDateTime cursor, Pageable pageable);

    Page<ChatMessageDto> listMessages(UUID conversationId, UUID userId, Pageable pageable);
}
