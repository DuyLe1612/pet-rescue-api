package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.repository.ConversationRepository;
import com.uit.petrescueapi.domain.valueobject.ConversationType;
import com.uit.petrescueapi.infrastructure.persistence.mapper.ChatEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.ConversationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationJpaRepository jpaRepository;
    private final ChatEntityMapper mapper;

    @Override
    public Conversation save(Conversation conversation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(conversation)));
    }

    @Override
    public Optional<Conversation> findById(UUID conversationId) {
        return jpaRepository.findByIdAndNotDeleted(conversationId).map(mapper::toDomain);
    }

    @Override
    public Optional<UUID> findDirectConversationId(UUID userId, UUID otherUserId, ConversationType type, UUID relatedEntityId) {
        return jpaRepository.findDirectConversationId(userId, otherUserId, type.name(), relatedEntityId);
    }
}
