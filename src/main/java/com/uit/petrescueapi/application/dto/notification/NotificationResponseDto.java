package com.uit.petrescueapi.application.dto.notification;

import com.uit.petrescueapi.domain.entity.Notification;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private UUID notificationId;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, String> data;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification n) {
        return NotificationResponseDto.builder()
                .notificationId(n.getNotificationId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .data(n.getData())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
