package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.valueobject.ConversationType;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID conversationId);

    Optional<UUID> findDirectConversationId(UUID userId, UUID otherUserId, ConversationType type, UUID relatedEntityId);
}
