package com.uit.petrescueapi.application.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MarkReadRequestDto {
    @NotNull
    private UUID messageId;
}
