package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.ConversationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    private UUID id;
    private ConversationType type;
    private String name;
    private UUID relatedEntityId;
    private String relatedInfo;

    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
