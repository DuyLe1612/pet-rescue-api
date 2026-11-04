package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;

import java.util.List;
import java.util.UUID;

public interface ChatRealtimePort {

    void publishMessage(List<UUID> recipientIds, UUID conversationId, ChatMessageDto message);

    void publishReadReceipt(List<UUID> recipientIds, UUID conversationId, UUID readerId);

    void publishTyping(List<UUID> recipientIds, UUID conversationId, UUID senderId, boolean typing);

    void publishPresence(List<UUID> recipientIds, UUID userId, String status);

    /** Same socket as chat, because the client already keeps it open. */
    void publishNotification(UUID recipientId, Object notification);

    void publishConversationUpdate(List<UUID> recipientIds, UUID conversationId, java.util.Map<String, Object> payload);
}
