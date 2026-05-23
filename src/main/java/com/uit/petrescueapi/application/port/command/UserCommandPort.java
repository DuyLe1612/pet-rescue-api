package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.domain.entity.User;

import java.util.UUID;

public interface UserCommandPort {
    User updateProfile(UUID userId, String username, String avatarUrl);
    void updatePushToken(UUID userId, String expoPushToken);
    void deactivate(UUID userId);
    User lockAccount(UUID userId);
    User unlockAccount(UUID userId);
}
