package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.out.ChatConversationCachePort;
import com.uit.petrescueapi.application.port.out.ChatQueryDataPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.domain.repository.ChatMessageRepository;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.domain.repository.ConversationRepository;
import com.uit.petrescueapi.infrastructure.persistence.adapter.ChatMessageRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.adapter.ChatQueryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.adapter.ConversationParticipantRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.adapter.ConversationRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mapper.ChatEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MessageBucketReader;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoChatMessageRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoChatQueryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoConversationParticipantRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoConversationRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.repository.ChatMessageJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.ConversationJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.ConversationParticipantJpaRepository;
import com.uit.petrescueapi.infrastructure.redis.NoOpChatConversationCacheAdapter;
import com.uit.petrescueapi.infrastructure.redis.RedisChatConversationCacheAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Exactly one implementation of every chat port is active per {@code app.storage.chat}. */
class ChatStorageWiringTest {

    @SuppressWarnings("unchecked")
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ConversationRepositoryAdapter.class, ConversationParticipantRepositoryAdapter.class,
                    ChatMessageRepositoryAdapter.class, ChatQueryAdapter.class, RedisChatConversationCacheAdapter.class,
                    MongoConversationRepositoryAdapter.class, MongoConversationParticipantRepositoryAdapter.class,
                    MongoChatMessageRepositoryAdapter.class, MongoChatQueryAdapter.class, MessageBucketReader.class,
                    NoOpChatConversationCacheAdapter.class)
            .withBean(ConversationJpaRepository.class, () -> mock(ConversationJpaRepository.class))
            .withBean(ConversationParticipantJpaRepository.class, () -> mock(ConversationParticipantJpaRepository.class))
            .withBean(ChatMessageJpaRepository.class, () -> mock(ChatMessageJpaRepository.class))
            .withBean(ChatEntityMapper.class, () -> mock(ChatEntityMapper.class))
            .withBean(RedisTemplate.class, () -> mock(RedisTemplate.class))
            .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
            .withBean(UserSnapshotPort.class, () -> mock(UserSnapshotPort.class));

    @Test
    void postgresIsTheDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(ConversationRepository.class)).isInstanceOf(ConversationRepositoryAdapter.class);
            assertThat(ctx.getBean(ConversationParticipantRepository.class)).isInstanceOf(ConversationParticipantRepositoryAdapter.class);
            assertThat(ctx.getBean(ChatMessageRepository.class)).isInstanceOf(ChatMessageRepositoryAdapter.class);
            assertThat(ctx.getBean(ChatQueryDataPort.class)).isInstanceOf(ChatQueryAdapter.class);
            assertThat(ctx.getBean(ChatConversationCachePort.class)).isInstanceOf(RedisChatConversationCacheAdapter.class);
        });
    }

    @Test
    void mongoModeSwapsEveryChatPortAndDropsTheRedisCache() {
        runner.withPropertyValues("app.storage.chat=mongo").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(ConversationRepository.class)).isInstanceOf(MongoConversationRepositoryAdapter.class);
            assertThat(ctx.getBean(ConversationParticipantRepository.class)).isInstanceOf(MongoConversationParticipantRepositoryAdapter.class);
            assertThat(ctx.getBean(ChatMessageRepository.class)).isInstanceOf(MongoChatMessageRepositoryAdapter.class);
            assertThat(ctx.getBean(ChatQueryDataPort.class)).isInstanceOf(MongoChatQueryAdapter.class);
            assertThat(ctx.getBean(ChatConversationCachePort.class)).isInstanceOf(NoOpChatConversationCacheAdapter.class);
        });
    }
}
