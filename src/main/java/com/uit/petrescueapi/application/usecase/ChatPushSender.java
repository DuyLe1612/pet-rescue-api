package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.port.out.PushNotificationPort;
import com.uit.petrescueapi.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sends on the application's async pool: blocking I/O must not run on the JVM's common pool. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPushSender {

    private final UserRepository users;
    private final PushNotificationPort push;

    @Async
    public void sendNewMessage(List<UUID> recipients, UUID conversationId, String content) {
        try {
            List<String> tokens = List.copyOf(users.findExpoPushTokensByIds(recipients).values());
            if (!tokens.isEmpty()) {
                push.sendPushToTokens(tokens, "New message", content, Map.of("conversationId", conversationId.toString()));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to send push notifications for conversation {}", conversationId, e);
        }
    }
}
