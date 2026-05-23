package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.ChatMessageJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, UUID> {

    Page<ChatMessageJpaEntity> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE ChatMessageJpaEntity m SET m.seen = true WHERE m.conversationId = :conversationId AND m.senderId <> :viewerId AND m.sentAt <= :readAt")
    void markMessagesSeen(@Param("conversationId") UUID conversationId,
                          @Param("viewerId") UUID viewerId,
                          @Param("readAt") LocalDateTime readAt);
}
