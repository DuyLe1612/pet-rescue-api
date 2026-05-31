package com.uit.petrescueapi.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.port.out.ChatRealtimePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketNotifier implements ChatRealtimePort {

    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void publishMessage(List<UUID> recipientIds, UUID conversationId, ChatMessageDto message) {
        ChatSocketEvent event = ChatSocketEvent.builder()
                .type("message")
                .conversationId(conversationId)
                .payload(message)
                .build();
        broadcast(recipientIds, event);
    }

    @Override
    public void publishReadReceipt(List<UUID> recipientIds, UUID conversationId, UUID readerId) {
        ChatSocketEvent event = ChatSocketEvent.builder()
                .type("read")
                .conversationId(conversationId)
                .readerId(readerId)
                .build();
        broadcast(recipientIds, event);
    }

    @Override
    public void publishTyping(List<UUID> recipientIds, UUID conversationId, UUID senderId, boolean typing) {
        ChatSocketEvent event = ChatSocketEvent.builder()
                .type("typing")
                .conversationId(conversationId)
                .readerId(senderId)
                .payload(java.util.Map.of("typing", typing))
                .build();
        broadcast(recipientIds, event);
    }

    @Override
    public void publishPresence(List<UUID> recipientIds, UUID userId, String status) {
        ChatSocketEvent event = ChatSocketEvent.builder()
                .type("presence")
                .userId(userId)
                .payload(java.util.Map.of("status", status))
                .build();
        broadcast(recipientIds, event);
    }

    @Override
    public void publishConversationUpdate(List<UUID> recipientIds, UUID conversationId, java.util.Map<String, Object> payload) {
        ChatSocketEvent event = ChatSocketEvent.builder()
                .type("conversation_update")
                .conversationId(conversationId)
                .payload(payload)
                .build();
        broadcast(recipientIds, event);
    }

    private void broadcast(List<UUID> userIds, ChatSocketEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            TextMessage message = new TextMessage(payload);
            for (UUID userId : userIds) {
                for (WebSocketSession session : sessionRegistry.getSessions(userId)) {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to broadcast chat event", ex);
        }
    }
}
