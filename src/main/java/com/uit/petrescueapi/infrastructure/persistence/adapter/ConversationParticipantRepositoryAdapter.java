package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.ConversationParticipant;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.ChatEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.ConversationParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConversationParticipantRepositoryAdapter implements ConversationParticipantRepository {

    private final ConversationParticipantJpaRepository jpaRepository;
    private final ChatEntityMapper mapper;

    @Override
    public ConversationParticipant save(ConversationParticipant participant) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(participant)));
    }

    @Override
    public Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId) {
        return jpaRepository.findByConversationIdAndUserId(conversationId, userId).map(mapper::toDomain);
    }

    @Override
    public List<ConversationParticipant> findByConversationId(UUID conversationId) {
        return mapper.toDomainParticipants(jpaRepository.findByConversationId(conversationId));
    }

    @Override
    public void updateUnreadCount(UUID conversationId, UUID userId, int unreadCount, LocalDateTime lastReadAt) {
        jpaRepository.updateUnreadCount(conversationId, userId, unreadCount, lastReadAt);
    }
}
