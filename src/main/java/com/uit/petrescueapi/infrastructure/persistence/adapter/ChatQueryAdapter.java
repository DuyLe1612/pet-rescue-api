package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatQueryDataPort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.infrastructure.persistence.projection.ConversationSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.ChatMessageJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.ConversationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryAdapter implements ChatQueryDataPort {

    private final ConversationJpaRepository conversationRepository;
    private final ChatMessageJpaRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;

    @Override
    public Page<ConversationSummaryDto> listConversationsByCursor(UUID userId, LocalDateTime cursor, Pageable pageable) {
        return conversationRepository.findSummariesByUserCursor(userId, cursor, pageable).map(this::toDto);
    }

    @Override
    public Page<ChatMessageDto> listMessages(UUID conversationId, UUID userId, Pageable pageable) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException("User not in conversation", "CHAT_FORBIDDEN"));

        return messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable)
                .map(message -> ChatMessageDto.builder()
                        .id(message.getMessageId())
                        .senderId(message.getSenderId())
                        .content(message.getContent())
                        .time(message.getSentAt())
                        .seen(message.isSeen())
                        .build());
    }

    private ConversationSummaryDto toDto(ConversationSummaryProjection projection) {
        return ConversationSummaryDto.builder()
                .id(projection.getId())
                .type(projection.getType())
                .name(projection.getName())
                .lastMessage(projection.getLastMessage())
                .lastTime(projection.getLastTime())
                .unread(projection.getUnread() == null ? 0 : projection.getUnread())
                .relatedInfo(projection.getRelatedInfo())
                .relatedEntityId(projection.getRelatedEntityId())
                .build();
    }
}
