package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatQueryDataPort;
import com.uit.petrescueapi.application.port.out.ChatConversationCachePort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.infrastructure.persistence.entity.ChatMessageJpaEntity;
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
    private final ChatConversationCachePort conversationCachePort;

    @Override
    public Page<ConversationSummaryDto> listConversationsByCursor(UUID userId, LocalDateTime cursor, Pageable pageable) {
        int size = pageable.getPageSize();
        var cached = conversationCachePort.getConversations(userId, cursor, size);
        if (cached.isPresent()) {
            return new org.springframework.data.domain.PageImpl<>(cached.get(), pageable, cached.get().size());
        }
        Page<ConversationSummaryDto> page = conversationRepository.findSummariesByUserCursor(userId, cursor, pageable).map(this::toDto);
        conversationCachePort.cacheConversations(userId, page.getContent());
        return page;
    }

    @Override
        public Page<ChatMessageDto> listMessagesByCursor(UUID conversationId, UUID userId, LocalDateTime cursor, Long cursorSeq, String direction, Pageable pageable) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException("User not in conversation", "CHAT_FORBIDDEN"));

            boolean isAfter = direction != null && direction.equalsIgnoreCase("after");
            Page<ChatMessageJpaEntity> page;
            if (isAfter) {
                if (cursorSeq == null) {
                    page = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);
                } else {
                    page = messageRepository.findByConversationIdAfterSeq(conversationId, cursorSeq, pageable);
                }
            } else {
                if (cursorSeq == null) {
                    page = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);
                } else {
                    page = messageRepository.findByConversationIdBeforeSeq(conversationId, cursorSeq, pageable);
                }
            }

        return page
                .map(message -> ChatMessageDto.builder()
                        .id(message.getMessageId())
                        .senderId(message.getSenderId())
                        .content(message.getContent())
                        .time(message.getSentAt())
                .messageSeq(message.getMessageSeq())
                        .seen(message.isSeen())
                        .build());
    }

    private ConversationSummaryDto toDto(ConversationSummaryProjection projection) {
        return ConversationSummaryDto.builder()
                .id(projection.getId())
                .type(projection.getType())
                .name(projection.getName())
                .otherUserId(projection.getOtherUserId())
                .otherUserName(projection.getOtherUserName())
                .otherUserAvatarUrl(projection.getOtherUserAvatarUrl())
                .lastMessage(projection.getLastMessage())
                .lastTime(projection.getLastTime())
                .unread(projection.getUnread() == null ? 0 : projection.getUnread())
                .relatedInfo(projection.getRelatedInfo())
                .relatedEntityId(projection.getRelatedEntityId())
                .build();
    }
}
