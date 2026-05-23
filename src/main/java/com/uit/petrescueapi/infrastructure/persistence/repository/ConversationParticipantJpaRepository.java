package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.ConversationParticipantId;
import com.uit.petrescueapi.infrastructure.persistence.entity.ConversationParticipantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantJpaRepository extends JpaRepository<ConversationParticipantJpaEntity, ConversationParticipantId> {

    Optional<ConversationParticipantJpaEntity> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipantJpaEntity> findByConversationId(UUID conversationId);

    @Modifying
    @Query("UPDATE ConversationParticipantJpaEntity cp SET cp.unreadCount = :unreadCount, cp.lastReadAt = :lastReadAt WHERE cp.conversationId = :conversationId AND cp.userId = :userId")
    void updateUnreadCount(@Param("conversationId") UUID conversationId,
                           @Param("userId") UUID userId,
                           @Param("unreadCount") int unreadCount,
                           @Param("lastReadAt") LocalDateTime lastReadAt);
}
