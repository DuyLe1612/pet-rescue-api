package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.CreateConversationRequestDto;
import com.uit.petrescueapi.application.dto.chat.CreateMessageRequestDto;
import com.uit.petrescueapi.application.dto.chat.MarkReadRequestDto;
import com.uit.petrescueapi.application.port.command.ChatCommandPort;
import com.uit.petrescueapi.application.port.out.ChatConversationCachePort;
import com.uit.petrescueapi.application.port.out.ChatRealtimePort;
import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.entity.ConversationParticipant;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.domain.service.ChatDomainService;
import com.uit.petrescueapi.domain.valueobject.ConversationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatCommandUseCase implements ChatCommandPort {

    private final ChatDomainService chatDomainService;
    private final ConversationParticipantRepository participantRepository;
    private final ChatRealtimePort chatRealtimePort;
    private final ChatConversationCachePort conversationCachePort;
    private final com.uit.petrescueapi.application.port.out.PushNotificationPort pushNotificationPort;
    private final com.uit.petrescueapi.domain.repository.UserRepository userRepository;

    @Override
    public Conversation createConversation(CreateConversationRequestDto request, UUID requesterId) {
        if (request.getParticipantId() == null) {
            throw new BusinessException("ParticipantId is required", "CHAT_PARTICIPANT_REQUIRED");
        }
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new BusinessException("Conversation type is required", "CHAT_TYPE_REQUIRED");
        }
        ConversationType type;
        try {
            type = ConversationType.valueOf(request.getType().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("Invalid conversation type", "CHAT_TYPE_INVALID");
        }
        Conversation conversation = chatDomainService.createDirectConversation(
                requesterId,
                request.getParticipantId(),
                type,
                request.getRescueCaseId(),
                request.getName(),
                request.getRelatedInfo()
        );
        conversationCachePort.evictUser(requesterId);
        conversationCachePort.evictUser(request.getParticipantId());
        return conversation;
    }

    @Override
    public ChatMessage sendMessage(UUID conversationId, CreateMessageRequestDto request, UUID senderId) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("Message content is required", "CHAT_CONTENT_REQUIRED");
        }

        ChatMessage message = chatDomainService.sendMessage(conversationId, senderId, request.getContent().trim());

        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        List<UUID> recipients = participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(senderId))
                .toList();

        ChatMessageDto dto = ChatMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .time(message.getSentAt())
            .messageSeq(message.getMessageSeq())
                .seen(message.isSeen())
                .build();

        chatRealtimePort.publishMessage(recipients, conversationId, dto);

        java.util.Map<String, Object> convoPayload = java.util.Map.of(
            "lastMessage", dto.getContent(),
            "lastTime", dto.getTime(),
            "lastSenderId", dto.getSenderId(),
            "lastMessageSeq", dto.getMessageSeq()
        );
        chatRealtimePort.publishConversationUpdate(recipients, conversationId, convoPayload);
        chatRealtimePort.publishConversationUpdate(java.util.List.of(senderId), conversationId, convoPayload);

        recipients.forEach(conversationCachePort::evictUser);
        conversationCachePort.evictUser(senderId);

        // collect expo push tokens and send push
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                List<String> tokens = recipients.stream()
                        .map(userRepository::findExpoPushTokenById)
                        .flatMap(java.util.Optional::stream)
                        .filter(token -> token != null && !token.isBlank())
                        .toList();
                if (!tokens.isEmpty()) {
                    String title = "New message";
                    String body = dto.getContent();
                    java.util.Map<String, Object> data = java.util.Map.of("conversationId", conversationId.toString());
                    pushNotificationPort.sendPushToTokens(tokens, title, body, data);
                }
            } catch (Exception ex) {
                log.warn("Failed to send push notifications", ex);
            }
        });
        return message;
    }

    @Override
    public void markRead(UUID conversationId, MarkReadRequestDto request, UUID userId) {
        chatDomainService.markRead(conversationId, userId);

        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        List<UUID> recipients = participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(userId))
                .toList();

        chatRealtimePort.publishReadReceipt(recipients, conversationId, userId);

        recipients.forEach(conversationCachePort::evictUser);
        conversationCachePort.evictUser(userId);
    }

    @Override
    public void deleteMessage(UUID conversationId, UUID messageId, UUID userId) {
        Conversation conversation = chatDomainService.deleteMessage(conversationId, messageId, userId);

        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        List<UUID> recipients = participants.stream()
                .map(ConversationParticipant::getUserId)
                .toList();

        java.util.Map<String, Object> convoPayload = java.util.Map.of(
                "lastMessage", conversation.getLastMessagePreview(),
                "lastTime", conversation.getLastMessageAt(),
                "lastSenderId", conversation.getLastMessageSenderId(),
                "lastMessageSeq", conversation.getLastMessageSeq()
        );
        chatRealtimePort.publishConversationUpdate(recipients, conversationId, convoPayload);

        recipients.forEach(conversationCachePort::evictUser);
    }
}
