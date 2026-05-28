package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.ConversationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.ConversationSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    @Query("SELECT c FROM ConversationJpaEntity c WHERE c.conversationId = :id AND c.deleted = false")
    Optional<ConversationJpaEntity> findByIdAndNotDeleted(@Param("id") UUID id);

    @Query(value = """
            SELECT c.conversation_id AS id,
                   c.type AS type,
           COALESCE(NULLIF(u.full_name, ''), u.username, c.name) AS name,
           u.user_id AS otherUserId,
           COALESCE(NULLIF(u.full_name, ''), u.username) AS otherUserName,
           u.avatar_url AS otherUserAvatarUrl,
                   c.related_info AS relatedInfo,
                   c.related_entity_id AS relatedEntityId,
                   lm.content AS lastMessage,
                   COALESCE(lm.sent_at, c.created_at) AS lastTime,
                   cp.unread_count AS unread
            FROM conversation_participants cp
            JOIN conversations c ON c.conversation_id = cp.conversation_id
            LEFT JOIN conversation_participants cp_other
                   ON cp_other.conversation_id = cp.conversation_id
                  AND cp_other.user_id <> :userId
            LEFT JOIN users u ON u.user_id = cp_other.user_id
            LEFT JOIN LATERAL (
                SELECT m.content, m.sent_at
                FROM chat_messages m
                WHERE m.conversation_id = c.conversation_id
                ORDER BY m.sent_at DESC
                LIMIT 1
            ) lm ON true
            WHERE cp.user_id = :userId
              AND c.is_deleted = false
              AND (CAST(:cursor AS TIMESTAMP) IS NULL OR COALESCE(lm.sent_at, c.created_at) < CAST(:cursor AS TIMESTAMP))
            ORDER BY COALESCE(lm.sent_at, c.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM conversation_participants cp
            JOIN conversations c ON c.conversation_id = cp.conversation_id
            LEFT JOIN LATERAL (
                SELECT m.sent_at
                FROM chat_messages m
                WHERE m.conversation_id = c.conversation_id
                ORDER BY m.sent_at DESC
                LIMIT 1
            ) lm ON true
            WHERE cp.user_id = :userId
              AND c.is_deleted = false
              AND (CAST(:cursor AS TIMESTAMP) IS NULL OR COALESCE(lm.sent_at, c.created_at) < CAST(:cursor AS TIMESTAMP))
            """,
            nativeQuery = true)
    Page<ConversationSummaryProjection> findSummariesByUserCursor(@Param("userId") UUID userId,
                                                                  @Param("cursor") java.time.LocalDateTime cursor,
                                                                  Pageable pageable);

    @Query(value = """
            SELECT c.conversation_id
            FROM conversation_participants cp1
            JOIN conversation_participants cp2 ON cp1.conversation_id = cp2.conversation_id
            JOIN conversations c ON c.conversation_id = cp1.conversation_id
            WHERE cp1.user_id = :userId
              AND cp2.user_id = :otherUserId
              AND c.type = :type
              AND (c.related_entity_id = :relatedEntityId OR (:relatedEntityId IS NULL AND c.related_entity_id IS NULL))
              AND (SELECT COUNT(*) FROM conversation_participants cp3 WHERE cp3.conversation_id = cp1.conversation_id) = 2
            LIMIT 1
            """,
            nativeQuery = true)
    Optional<UUID> findDirectConversationId(@Param("userId") UUID userId,
                                           @Param("otherUserId") UUID otherUserId,
                                           @Param("type") String type,
                                           @Param("relatedEntityId") UUID relatedEntityId);
}
