package com.uit.petrescueapi.application.dto.chat;

import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import com.uit.petrescueapi.application.support.CanonicalEnum;
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
    private String requesterName;
    private String requesterAvatarUrl;
    @NotNull
    private UUID addresseeId;
    @NotBlank
    @CanonicalEnum(FriendRequestStatus.class)
    private String status;
    private LocalDateTime createdAt;
}
