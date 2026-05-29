package com.uit.petrescueapi.application.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FriendSummaryDto {
    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
