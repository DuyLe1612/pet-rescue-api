package com.uit.petrescueapi.infrastructure.redis;

import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatConversationCachePort;
import com.uit.petrescueapi.infrastructure.config.ChatOnMongo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * With chat on MongoDB the conversation list is a single indexed read of {@code inboxes},
 * so the Redis cache (and its invalidation) is no longer needed.
 */
@Component
@ChatOnMongo
public class NoOpChatConversationCacheAdapter implements ChatConversationCachePort {

    @Override
    public Optional<List<ConversationSummaryDto>> getConversations(UUID userId, LocalDateTime cursor, int size) {
        return Optional.empty();
    }

    @Override
    public void cacheConversations(UUID userId, List<ConversationSummaryDto> items) {
    }

    @Override
    public void evictUser(UUID userId) {
    }
}
