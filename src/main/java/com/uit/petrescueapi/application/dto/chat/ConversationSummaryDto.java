package com.uit.petrescueapi.application.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ConversationSummaryDto {
    private UUID id;
    private String type;
    private String name;
    private String lastMessage;
    private LocalDateTime lastTime;
    private int unread;
    private String relatedInfo;
    private UUID relatedEntityId;
}
