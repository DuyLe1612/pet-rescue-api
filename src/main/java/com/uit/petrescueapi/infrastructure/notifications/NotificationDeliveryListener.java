package com.uit.petrescueapi.infrastructure.notifications;

import com.uit.petrescueapi.application.dto.notification.NotificationResponseDto;
import com.uit.petrescueapi.application.port.out.ChatRealtimePort;
import com.uit.petrescueapi.application.port.out.PushNotificationPort;
import com.uit.petrescueapi.domain.entity.Notification;
import com.uit.petrescueapi.domain.event.NotificationsCreatedEvent;
import com.uit.petrescueapi.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/** Best effort: the notification is already stored, so a failed delivery loses nothing. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryListener {

    private final ChatRealtimePort realtime;
    private final PushNotificationPort push;
    private final UserRepository users;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(NotificationsCreatedEvent event) {
        for (Notification notification : event.notifications()) {
            try {
                realtime.publishNotification(notification.getUserId(), NotificationResponseDto.from(notification));
                users.findExpoPushTokenById(notification.getUserId()).ifPresent(token ->
                        push.sendPushToTokens(List.of(token), notification.getTitle(), notification.getBody(),
                                Map.of("type", notification.getType().name(),
                                        "data", notification.getData() == null ? Map.of() : notification.getData())));
            } catch (RuntimeException e) {
                log.warn("Could not deliver notification {}: {}", notification.getNotificationId(), e.getMessage());
            }
        }
    }
}
