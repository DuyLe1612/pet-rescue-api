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
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration. Redis is used for:
 * - Spring Cache for the admin dashboard ({@code @Cacheable} in {@code AdminDashboardUsecase},
 *   per-cache TTLs below)
 * - Login lockout counters ({@code LoginAttemptService}, uses {@code StringRedisTemplate})
 * - Like de-duplication for the RabbitMQ counter listeners ({@code RedisCounterService}, ZSET).
 *   The counts users see are stored in MongoDB ({@code stats.*}), not read from Redis.
 *
 * <p>The {@link GenericJackson2JsonRedisSerializer} is exposed as a singleton
 * bean so it is created once. {@code cacheManager()} does not use it, see {@link #cacheValueSerializer()}.</p>
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
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("admin:dashboard", cacheConfig(Duration.ofMinutes(5)));
        configs.put("admin:rescue-stats", cacheConfig(Duration.ofMinutes(15)));
        configs.put("admin:province-stats", cacheConfig(Duration.ofHours(1)));
        configs.put("admin:pending-tasks", cacheConfig(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(factory)
                .withInitialCacheConfigurations(configs)
                .build();
    }

    /**
     * JDK serialization, because the JSON serializer returned a {@code LinkedHashMap} that failed
     * with a ClassCastException on every cache hit. Cached dashboard DTOs must be {@code Serializable}.
     */
    RedisSerializer<Object> cacheValueSerializer() {
        return new JdkSerializationRedisSerializer(getClass().getClassLoader());
    }

    private RedisCacheConfiguration cacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith("v2:")     // values written in the old format stay unread until they expire
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheValueSerializer()));
    }
}
