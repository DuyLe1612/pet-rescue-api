package com.uit.petrescueapi.application.dto.chat;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FriendRequestDto {
    @NotNull
    private UUID id;
    @NotNull
    private UUID requesterId;
    @NotNull
    private UUID addresseeId;
    @NotBlank
    private String status;
    private LocalDateTime createdAt;
}
