package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.chat.CreateConversationRequestDto;
import com.uit.petrescueapi.application.dto.chat.CreateMessageRequestDto;
import com.uit.petrescueapi.application.dto.chat.MarkReadRequestDto;
import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.entity.Conversation;

import java.util.UUID;

public interface ChatCommandPort {

    Conversation createConversation(CreateConversationRequestDto request, UUID requesterId);

    ChatMessage sendMessage(UUID conversationId, CreateMessageRequestDto request, UUID senderId);

    void markRead(UUID conversationId, MarkReadRequestDto request, UUID userId);

    void deleteMessage(UUID conversationId, UUID messageId, UUID userId);
}
