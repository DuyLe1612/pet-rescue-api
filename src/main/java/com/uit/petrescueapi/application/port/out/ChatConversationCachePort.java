package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationCachePort {

    Optional<List<ConversationSummaryDto>> getConversations(UUID userId, LocalDateTime cursor, int size);

    void cacheConversations(UUID userId, List<ConversationSummaryDto> items);

    void evictUser(UUID userId);
}
