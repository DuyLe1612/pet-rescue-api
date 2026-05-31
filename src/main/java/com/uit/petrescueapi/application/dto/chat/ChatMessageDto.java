package com.uit.petrescueapi.application.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatMessageDto {
    private UUID id;
    private UUID senderId;
    private String content;
    private LocalDateTime time;
    private Long messageSeq;
    private boolean seen;
}
