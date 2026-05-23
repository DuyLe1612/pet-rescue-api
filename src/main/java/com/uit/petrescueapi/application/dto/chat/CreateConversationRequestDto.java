package com.uit.petrescueapi.application.dto.chat;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateConversationRequestDto {
    private String type;
    private UUID participantId;
    private UUID rescueCaseId;
    private String name;
    private String relatedInfo;
}
