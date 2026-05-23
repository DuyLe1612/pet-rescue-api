package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {

    private UUID id;
    private UUID requesterId;
    private UUID addresseeId;
    private FriendRequestStatus status;
    private LocalDateTime respondedAt;

    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
