package com.uit.petrescueapi.infrastructure.websocket;

import lombok.Data;

import java.util.UUID;

@Data
public class ChatSocketRequest {
    private String type;
    private UUID conversationId;
    private String content;
    private Object payload;
}
