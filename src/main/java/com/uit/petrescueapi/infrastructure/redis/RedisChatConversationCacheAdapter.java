package com.uit.petrescueapi.infrastructure.redis;

import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatConversationCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisChatConversationCacheAdapter implements ChatConversationCachePort {

    private final RedisTemplate<String, Object> redis;

    @Value("${app.redis.key.chat-conversations-zset:chat:%s:conversations}")
    private String conversationZsetKeyPattern;

    @Value("${app.redis.key.chat-conversation-hash:chat:%s:conversation_data}")
    private String conversationHashKeyPattern;

    @Value("${app.redis.chat-conversations-ttl-seconds:300}")
    private long ttlSeconds;

    @Override
    public Optional<List<ConversationSummaryDto>> getConversations(UUID userId, LocalDateTime cursor, int size) {
        String zsetKey = zsetKey(userId);
        if (!Boolean.TRUE.equals(redis.hasKey(zsetKey))) {
            return Optional.empty();
        }

        double maxScore = cursor == null ? Double.POSITIVE_INFINITY : toScore(cursor) - 1;
        Set<Object> ids = redis.opsForZSet().reverseRangeByScore(zsetKey, Double.NEGATIVE_INFINITY, maxScore, 0, size);
        if (ids == null) {
            return Optional.of(List.of());
        }

        String hashKey = hashKey(userId);
        List<ConversationSummaryDto> items = new ArrayList<>(ids.size());
        for (Object idObj : ids) {
            Object value = redis.opsForHash().get(hashKey, idObj.toString());
            if (!(value instanceof ConversationSummaryDto dto)) {
                return Optional.empty();
            }
            items.add(dto);
        }
        return Optional.of(items);
    }

    @Override
    public void cacheConversations(UUID userId, List<ConversationSummaryDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String zsetKey = zsetKey(userId);
        String hashKey = hashKey(userId);
        for (ConversationSummaryDto item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            double score = toScore(item.getLastTime());
            redis.opsForZSet().add(zsetKey, item.getId().toString(), score);
            redis.opsForHash().put(hashKey, item.getId().toString(), item);
        }
        if (ttlSeconds > 0) {
            Duration ttl = Duration.ofSeconds(ttlSeconds);
            redis.expire(zsetKey, ttl);
            redis.expire(hashKey, ttl);
        }
    }

    @Override
    public void evictUser(UUID userId) {
        redis.delete(zsetKey(userId));
        redis.delete(hashKey(userId));
    }

    private String zsetKey(UUID userId) {
        return String.format(conversationZsetKeyPattern, userId);
    }

    private String hashKey(UUID userId) {
        return String.format(conversationHashKeyPattern, userId);
    }

    private double toScore(LocalDateTime time) {
        if (time == null) {
            return 0D;
        }
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
