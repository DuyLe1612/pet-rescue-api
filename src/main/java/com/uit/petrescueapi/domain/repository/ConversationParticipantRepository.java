package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.ConversationParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantRepository {

    ConversationParticipant save(ConversationParticipant participant);

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    void updateUnreadCount(UUID conversationId, UUID userId, int unreadCount, java.time.LocalDateTime lastReadAt);
}
