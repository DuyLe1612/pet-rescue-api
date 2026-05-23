package com.uit.petrescueapi.application.dto.chat;

import lombok.Data;

import java.util.UUID;

@Data
public class MarkReadRequestDto {
    private UUID messageId;
}
