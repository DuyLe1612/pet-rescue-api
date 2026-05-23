package com.uit.petrescueapi.infrastructure.persistence.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConversationParticipantId implements Serializable {
    private UUID conversationId;
    private UUID userId;
}
