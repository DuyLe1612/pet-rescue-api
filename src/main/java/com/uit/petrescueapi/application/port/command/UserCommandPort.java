package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.application.dto.user.UpdateUserProfileRequestDto;

import java.util.UUID;

public interface UserCommandPort {
    User updateProfile(UUID userId, UpdateUserProfileRequestDto request);
    void updatePushToken(UUID userId, String expoPushToken);
    void deactivate(UUID userId);
    User lockAccount(UUID userId);
    User unlockAccount(UUID userId);
}
