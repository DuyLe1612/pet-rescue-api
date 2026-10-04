package com.uit.petrescueapi.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching and counter management.
 * Used for:
 * - Denormalized like counters (ZSET for idempotency)
 * - Comment counters
 * - Fast read-path for social media features
 *
 * <p>The {@link GenericJackson2JsonRedisSerializer} is exposed as a singleton
 * bean so it is created once and reused by both {@code redisTemplate()} and
 * {@code cacheManager()} — the previous implementation allocated a fresh
 * serializer instance inside each {@code @Bean} method.</p>
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public GenericJackson2JsonRedisSerializer redisJsonSerializer(ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       GenericJackson2JsonRedisSerializer redisJsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(redisJsonSerializer);
        template.setHashValueSerializer(redisJsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory,
                                          GenericJackson2JsonRedisSerializer redisJsonSerializer) {
        RedisSerializationContext.SerializationPair<Object> jsonPair =
                RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer);

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        configs.put(
                "admin:dashboard",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5))
                        .serializeValuesWith(jsonPair)
        );

        configs.put(
                "admin:rescue-stats",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(15))
                        .serializeValuesWith(jsonPair)
        );

        configs.put(
                "admin:province-stats",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .serializeValuesWith(jsonPair)
        );

        configs.put(
                "admin:pending-tasks",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(2))
                        .serializeValuesWith(jsonPair)
        );

        return RedisCacheManager.builder(factory)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}