package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.repository.ChatMessageRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.ChatEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.ChatMessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final ChatMessageJpaRepository jpaRepository;
    private final ChatEntityMapper mapper;

    @Override
    public ChatMessage save(ChatMessage message) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(message)));
    }

    @Override
    public Page<ChatMessage> findByConversationId(UUID conversationId, Pageable pageable) {
        return jpaRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable).map(mapper::toDomain);
    }

    @Override
    public void markMessagesSeen(UUID conversationId, UUID viewerId, LocalDateTime readAt) {
        jpaRepository.markMessagesSeen(conversationId, viewerId, readAt);
    }
}
