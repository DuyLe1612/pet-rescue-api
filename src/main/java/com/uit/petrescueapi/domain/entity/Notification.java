package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** In-app notification; web clients cannot receive Expo push. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private UUID notificationId;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, String> data;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
