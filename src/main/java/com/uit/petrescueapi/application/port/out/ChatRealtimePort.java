package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;

import java.util.List;
import java.util.UUID;

public interface ChatRealtimePort {

    void publishMessage(List<UUID> recipientIds, UUID conversationId, ChatMessageDto message);

    void publishReadReceipt(List<UUID> recipientIds, UUID conversationId, UUID readerId);

    void publishTyping(List<UUID> recipientIds, UUID conversationId, UUID senderId, boolean typing);

    void publishPresence(List<UUID> recipientIds, UUID userId, String status);
}
