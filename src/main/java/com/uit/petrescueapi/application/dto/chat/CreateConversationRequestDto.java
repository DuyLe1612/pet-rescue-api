package com.uit.petrescueapi.application.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateConversationRequestDto {
    @NotBlank
    @Size(max = 50)
    private String type;
    @NotNull
    private UUID participantId;
    private UUID rescueCaseId;
    @Size(max = 255)
    private String name;
    @Size(max = 1000)
    private String relatedInfo;
}
