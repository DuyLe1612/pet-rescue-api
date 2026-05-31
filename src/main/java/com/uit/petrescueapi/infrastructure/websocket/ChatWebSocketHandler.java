package com.uit.petrescueapi.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.CreateMessageRequestDto;
import com.uit.petrescueapi.application.dto.chat.MarkReadRequestDto;
import com.uit.petrescueapi.application.port.command.ChatCommandPort;
import com.uit.petrescueapi.application.port.out.ChatRealtimePort;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.domain.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ChatCommandPort chatCommandPort;
    private final ChatRealtimePort chatRealtimePort;
    private final ConversationParticipantRepository participantRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionRegistry.register(userId, session);
        log.debug("Chat WS connected: {}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID userId = getUserId(session);
        if (userId != null) {
            sessionRegistry.unregister(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        ChatSocketRequest request = objectMapper.readValue(message.getPayload(), ChatSocketRequest.class);
        if (request.getType() == null) {
            return;
        }

        switch (request.getType()) {
            case "send" -> handleSend(session, userId, request);
            case "read" -> handleRead(userId, request);
            case "typing" -> handleTyping(userId, request);
            case "presence" -> handlePresence(userId, request);
            default -> log.debug("Unhandled chat ws event: {}", request.getType());
        }
    }

    private void handleTyping(UUID userId, ChatSocketRequest request) {
        if (request.getConversationId() == null) return;
        boolean typing = request.getPayload() instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(request.getPayload()));
        try {
            var participants = participantRepository.findByConversationId(request.getConversationId());
            var recipients = participants.stream()
                    .map(p -> p.getUserId())
                    .filter(id -> !id.equals(userId))
                    .toList();
            if (!recipients.isEmpty()) {
                chatRealtimePort.publishTyping(recipients, request.getConversationId(), userId, typing);
            }
        } catch (Exception ex) {
            log.warn("Failed to publish typing to participants", ex);
        }
    }

    private void handlePresence(UUID userId, ChatSocketRequest request) {
        if (request.getConversationId() == null) return;
        String status = request.getContent();
        try {
            var participants = participantRepository.findByConversationId(request.getConversationId());
            var recipients = participants.stream()
                    .map(p -> p.getUserId())
                    .filter(id -> !id.equals(userId))
                    .toList();
            if (!recipients.isEmpty()) {
                chatRealtimePort.publishPresence(recipients, userId, status);
            }
        } catch (Exception ex) {
            log.warn("Failed to publish presence to participants", ex);
        }
    }

    private void handleSend(WebSocketSession session, UUID userId, ChatSocketRequest request) throws Exception {
        CreateMessageRequestDto cmd = new CreateMessageRequestDto();
        cmd.setContent(request.getContent());
        ChatMessage saved = chatCommandPort.sendMessage(request.getConversationId(), cmd, userId);

        ChatMessageDto dto = ChatMessageDto.builder()
                .id(saved.getId())
                .senderId(saved.getSenderId())
                .content(saved.getContent())
                .time(saved.getSentAt())
            .messageSeq(saved.getMessageSeq())
                .seen(saved.isSeen())
                .build();

        ChatSocketEvent ack = ChatSocketEvent.builder()
                .type("ack")
                .conversationId(request.getConversationId())
                .payload(dto)
                .build();

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
    }

    private void handleRead(UUID userId, ChatSocketRequest request) {
        MarkReadRequestDto cmd = new MarkReadRequestDto();
        chatCommandPort.markRead(request.getConversationId(), cmd, userId);
    }

    private UUID getUserId(WebSocketSession session) {
        Object raw = session.getAttributes().get("userId");
        if (raw instanceof UUID) {
            return (UUID) raw;
        }
        if (raw instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
