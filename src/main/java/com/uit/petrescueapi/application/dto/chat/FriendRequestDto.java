package com.uit.petrescueapi.application.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FriendRequestDto {
    private UUID id;
    private UUID requesterId;
    private UUID addresseeId;
    private String status;
    private LocalDateTime createdAt;
}
