package com.uit.petrescueapi.application.port.command;

import java.util.UUID;

public interface NotificationCommandPort {

    void markRead(UUID notificationId, UUID userId);

    int markAllRead(UUID userId);
}
