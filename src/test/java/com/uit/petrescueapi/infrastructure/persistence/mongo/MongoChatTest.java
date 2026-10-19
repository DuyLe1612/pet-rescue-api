package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatQueryDataPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.repository.UserRepository;
import com.uit.petrescueapi.domain.service.ChatDomainService;
import com.uit.petrescueapi.domain.valueobject.ConversationType;
import com.uit.petrescueapi.infrastructure.config.MongoConfig;
import com.uit.petrescueapi.infrastructure.config.StorageTransactionAliases;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MessageBucketReader;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoChatMessageRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoChatQueryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoConversationParticipantRepositoryAdapter;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.MongoConversationRepositoryAdapter;
import com.uit.petrescueapi.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(MongoChatTest.Config.class)
@TestPropertySource(properties = "app.storage.chat=mongo")
class MongoChatTest extends MongoIntegrationTest {

    static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @TestConfiguration
    @Import({MongoConversationRepositoryAdapter.class, MongoConversationParticipantRepositoryAdapter.class,
            MongoChatMessageRepositoryAdapter.class, MongoChatQueryAdapter.class, MessageBucketReader.class,
            ChatDomainService.class, MongoTransientRetryAspect.class})
    @org.springframework.transaction.annotation.EnableTransactionManagement
    @org.springframework.context.annotation.EnableAspectJAutoProxy
    static class Config {
        @Bean(MongoConfig.MONGO_TX)
        MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory f) {
            return new MongoTransactionManager(f);
        }

        @Bean
        static StorageTransactionAliases storageTransactionAliases() {
            return new StorageTransactionAliases();
        }

        @Bean
        UserRepository userRepository() {
            UserRepository repo = mock(UserRepository.class);
            when(repo.existsById(any())).thenReturn(true);
            return repo;
        }

        @Bean
        UserSnapshotPort userSnapshotPort() {
            Map<UUID, UserSnapshot> users = List.of(
                    new UserSnapshot(ALICE, "alice", "Alice Nguyen", "a.png"),
                    new UserSnapshot(BOB, "bob", "", "b.png"),
                    new UserSnapshot(CAROL, "carol", "Carol", null)
            ).stream().collect(Collectors.toMap(UserSnapshot::id, Function.identity()));
            return new UserSnapshotPort() {
                @Override
                public Map<UUID, UserSnapshot> findByIds(Collection<UUID> ids) {
                    return ids.stream().filter(users::containsKey).collect(Collectors.toMap(Function.identity(), users::get));
                }
            };
        }
    }

    @Autowired MongoTemplate mongo;
    @Autowired ChatDomainService chat;
    @Autowired ChatQueryDataPort query;

    @BeforeEach
    void clean() {
        mongo.getDb().drop();
        new MongoSchemaInitializer(mongo).run(null);
    }

    private Conversation direct(UUID a, UUID b) {
        return chat.createDirectConversation(a, b, ConversationType.GENERAL, null, null, null);
    }

    private List<ChatMessageDto> before(UUID conv, UUID viewer, Long cursorSeq, int size) {
        return query.listMessagesByCursor(conv, viewer, null, cursorSeq, "before", PageRequest.of(0, size)).getContent();
    }

    // ── conversations / inboxes ──────────────────────────────

    @Test
    void createDirectConversationCreatesOneInboxPerParticipantWithPeerSnapshot() {
        Conversation c = direct(ALICE, BOB);

        List<ConversationSummaryDto> aliceInbox = query.listConversationsByCursor(ALICE, null, PageRequest.of(0, 10)).getContent();
        assertThat(aliceInbox).singleElement().satisfies(s -> {
            assertThat(s.getId()).isEqualTo(c.getId());
            assertThat(s.getOtherUserId()).isEqualTo(BOB);
            assertThat(s.getOtherUserName()).isEqualTo("bob");          // blank full name falls back to username
            assertThat(s.getOtherUserAvatarUrl()).isEqualTo("b.png");
            assertThat(s.getUnread()).isZero();
        });
        assertThat(query.listConversationsByCursor(BOB, null, PageRequest.of(0, 10)).getContent())
                .singleElement().satisfies(s -> assertThat(s.getOtherUserName()).isEqualTo("Alice Nguyen"));
    }

    @Test
    void createDirectConversationIsIdempotentForSamePairTypeAndRelatedEntity() {
        Conversation first = direct(ALICE, BOB);
        Conversation again = direct(BOB, ALICE);
        Conversation rescue = chat.createDirectConversation(ALICE, BOB, ConversationType.RESCUE, UUID.randomUUID(), null, "Rescue: Q7");

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(rescue.getId()).isNotEqualTo(first.getId());
        assertThat(mongo.getCollection(MongoCollections.CONVERSATIONS).countDocuments()).isEqualTo(2);
        assertThat(mongo.getCollection(MongoCollections.INBOXES).countDocuments()).isEqualTo(4);
    }

    @Test
    void conversationListIsOrderedByLastMessageAndPagesByCursor() throws Exception {
        Conversation withBob = direct(ALICE, BOB);
        Conversation withCarol = direct(ALICE, CAROL);
        chat.sendMessage(withBob.getId(), BOB, "older");
        Thread.sleep(5);
        chat.sendMessage(withCarol.getId(), CAROL, "newer");

        List<ConversationSummaryDto> page1 = query.listConversationsByCursor(ALICE, null, PageRequest.of(0, 1)).getContent();
        assertThat(page1).extracting(ConversationSummaryDto::getId).containsExactly(withCarol.getId());
        assertThat(page1.get(0).getLastMessage()).isEqualTo("newer");
        assertThat(page1.get(0).getUnread()).isEqualTo(1);

        List<ConversationSummaryDto> page2 = query.listConversationsByCursor(ALICE, page1.get(0).getLastTime(), PageRequest.of(0, 1)).getContent();
        assertThat(page2).extracting(ConversationSummaryDto::getId).containsExactly(withBob.getId());
    }

    // ── messages / buckets ───────────────────────────────────

    @Test
    void sendMessageAssignsContiguousSeqAndSplitsBucketsEvery50() {
        Conversation c = direct(ALICE, BOB);
        for (int i = 1; i <= 51; i++) {
            ChatMessage m = chat.sendMessage(c.getId(), i % 2 == 0 ? ALICE : BOB, "m" + i);
            assertThat(m.getMessageSeq()).isEqualTo(i);
        }

        List<Document> buckets = mongo.getCollection(MongoCollections.MESSAGE_BUCKETS).find().into(new java.util.ArrayList<>());
        assertThat(buckets).extracting(d -> d.getString("_id"))
                .containsExactlyInAnyOrder(c.getId() + ":0", c.getId() + ":1");
        assertThat(query.listConversationsByCursor(ALICE, null, PageRequest.of(0, 5)).getContent().get(0).getUnread())
                .isEqualTo(1); // Alice sent m50, so only m51 from Bob is unread
    }

    @Test
    void historyPagesBackwardAcrossBucketBoundary() {
        Conversation c = direct(ALICE, BOB);
        for (int i = 1; i <= 60; i++) chat.sendMessage(c.getId(), BOB, "m" + i);

        List<ChatMessageDto> first = before(c.getId(), ALICE, null, 20);
        assertThat(first).extracting(ChatMessageDto::getMessageSeq).startsWith(60L).endsWith(41L).hasSize(20);

        List<ChatMessageDto> second = before(c.getId(), ALICE, 41L, 20);
        assertThat(second).extracting(ChatMessageDto::getMessageSeq).startsWith(40L).endsWith(21L);

        List<ChatMessageDto> last = before(c.getId(), ALICE, 3L, 20);
        assertThat(last).extracting(ChatMessageDto::getMessageSeq).containsExactly(2L, 1L);
    }

    @Test
    void afterDirectionReturnsNewerMessagesAscending() {
        Conversation c = direct(ALICE, BOB);
        for (int i = 1; i <= 55; i++) chat.sendMessage(c.getId(), BOB, "m" + i);

        List<ChatMessageDto> newer = query.listMessagesByCursor(c.getId(), ALICE, null, 48L, "after", PageRequest.of(0, 5)).getContent();
        assertThat(newer).extracting(ChatMessageDto::getMessageSeq).containsExactly(49L, 50L, 51L, 52L, 53L);
    }

    @Test
    void deletedMessagesAreSkippedButPageIsStillFull() {
        Conversation c = direct(ALICE, BOB);
        for (int i = 1; i <= 10; i++) chat.sendMessage(c.getId(), BOB, "m" + i);
        List<ChatMessageDto> all = before(c.getId(), BOB, null, 10);
        chat.deleteMessage(c.getId(), all.get(1).getId(), BOB);   // seq 9
        chat.deleteMessage(c.getId(), all.get(2).getId(), BOB);   // seq 8

        assertThat(before(c.getId(), BOB, null, 5)).extracting(ChatMessageDto::getMessageSeq)
                .containsExactly(10L, 7L, 6L, 5L, 4L);
    }

    @Test
    void deletingLastMessageRefreshesPreviewInBothInboxes() {
        Conversation c = direct(ALICE, BOB);
        chat.sendMessage(c.getId(), BOB, "hello");
        ChatMessage oops = chat.sendMessage(c.getId(), BOB, "oops");

        Conversation after = chat.deleteMessage(c.getId(), oops.getId(), BOB);

        assertThat(after.getLastMessagePreview()).isEqualTo("hello");
        assertThat(after.getLastMessageSeq()).isEqualTo(1L);
        assertThat(query.listConversationsByCursor(ALICE, null, PageRequest.of(0, 5)).getContent().get(0).getLastMessage()).isEqualTo("hello");
        assertThat(query.listConversationsByCursor(BOB, null, PageRequest.of(0, 5)).getContent().get(0).getLastMessage()).isEqualTo("hello");
    }

    @Test
    void onlySenderMayDeleteMessage() {
        Conversation c = direct(ALICE, BOB);
        ChatMessage m = chat.sendMessage(c.getId(), BOB, "mine");
        assertThatThrownBy(() -> chat.deleteMessage(c.getId(), m.getId(), ALICE)).isInstanceOf(BusinessException.class);
    }

    // ── read state ───────────────────────────────────────────

    @Test
    void seenIsDerivedFromRecipientReadWatermark() {
        Conversation c = direct(ALICE, BOB);
        chat.sendMessage(c.getId(), ALICE, "one");
        chat.sendMessage(c.getId(), ALICE, "two");
        assertThat(before(c.getId(), ALICE, null, 10)).allMatch(m -> !m.isSeen());

        chat.markRead(c.getId(), BOB);
        chat.sendMessage(c.getId(), ALICE, "three");

        assertThat(before(c.getId(), ALICE, null, 10))
                .extracting(ChatMessageDto::getMessageSeq, ChatMessageDto::isSeen)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3L, false),
                        org.assertj.core.groups.Tuple.tuple(2L, true),
                        org.assertj.core.groups.Tuple.tuple(1L, true));
        assertThat(query.listConversationsByCursor(BOB, null, PageRequest.of(0, 5)).getContent().get(0).getUnread()).isEqualTo(1);
    }

    @Test
    void concurrentSendersInSameConversationAllSucceedWithUniqueContiguousSeqs() throws Exception {
        Conversation c = direct(ALICE, BOB);
        int perSender = 15;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<List<Long>>> futures = new java.util.ArrayList<>();
        for (UUID sender : List.of(ALICE, BOB)) {
            futures.add(pool.submit(() -> {
                start.await();
                List<Long> seqs = new java.util.ArrayList<>();
                for (int i = 0; i < perSender; i++) seqs.add(chat.sendMessage(c.getId(), sender, "x").getMessageSeq());
                return seqs;
            }));
        }
        start.countDown();
        List<Long> all = new java.util.ArrayList<>();
        for (var f : futures) all.addAll(f.get(60, java.util.concurrent.TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(all).hasSize(2 * perSender).doesNotHaveDuplicates()
                .allMatch(seq -> seq >= 1 && seq <= 2 * perSender);
        assertThat(before(c.getId(), ALICE, null, 100)).hasSize(2 * perSender);
    }

    @Test
    void nonParticipantCannotSendOrRead() {
        Conversation c = direct(ALICE, BOB);
        assertThatThrownBy(() -> chat.sendMessage(c.getId(), CAROL, "hi")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> before(c.getId(), CAROL, null, 10)).isInstanceOf(BusinessException.class);
    }
}
