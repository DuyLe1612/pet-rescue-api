package com.uit.petrescueapi.infrastructure.websocket;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ChatSocketEvent {
    private String type;
    private UUID conversationId;
    private Object payload;
    private UUID readerId;
    private UUID userId;
}
