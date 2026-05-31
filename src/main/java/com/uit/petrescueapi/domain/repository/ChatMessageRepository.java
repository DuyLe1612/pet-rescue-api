package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    java.util.Optional<ChatMessage> findById(UUID messageId);

    Page<ChatMessage> findByConversationId(UUID conversationId, Pageable pageable);

    java.util.Optional<ChatMessage> findLatestByConversationId(UUID conversationId);

    void markMessagesSeen(UUID conversationId, UUID viewerId, java.time.LocalDateTime readAt);
}
