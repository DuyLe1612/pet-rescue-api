package com.uit.petrescueapi.infrastructure.persistence.entity;

import com.uit.petrescueapi.domain.valueobject.ConversationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversation_type", columnList = "type")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "conversation_id", updatable = false, nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConversationType type;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "related_info", columnDefinition = "TEXT")
    private String relatedInfo;

    @Column(name = "last_message_at", nullable = false)
    private java.time.LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", columnDefinition = "TEXT")
    private String lastMessagePreview;

    @Column(name = "last_message_sender_id")
    private UUID lastMessageSenderId;

    @Column(name = "last_message_seq")
    private Long lastMessageSeq;
}
