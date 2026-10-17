package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.ChatMessageRepository;
import com.uit.petrescueapi.infrastructure.config.ChatOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.ConversationDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.MessageBucketDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.MessageBucketDocument.MessageDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * {@link ChatMessageRepository} on message buckets. A new message reserves the next seq on its
 * conversation ({@code $inc}) and is pushed into the bucket that seq maps to (upsert creates the
 * next bucket every {@value MessageBucketDocument#SIZE} messages).
 */
@Component
@ChatOnMongo
@RequiredArgsConstructor
public class MongoChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final MongoTemplate mongo;
    private final MessageBucketReader reader;

    @Override
    public ChatMessage save(ChatMessage message) {
        return message.getMessageSeq() == null ? append(message) : updateExisting(message);
    }

    private ChatMessage append(ChatMessage message) {
        ConversationDocument conversation = mongo.findAndModify(
                query(where("_id").is(message.getConversationId())),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true),
                ConversationDocument.class);
        if (conversation == null) {
            throw new ResourceNotFoundException("Conversation", "id", message.getConversationId());
        }
        long seq = conversation.getSeq();
        message.setMessageSeq(seq);

        MessageDocument doc = MessageDocument.builder()
                .messageId(message.getId())
                .seq(seq)
                .senderId(message.getSenderId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .createdAt(message.getCreatedAt())
                .build();
        mongo.upsert(query(where("_id").is(MessageBucketDocument.idForSeq(message.getConversationId(), seq))),
                new Update()
                        .push("messages", doc)
                        .setOnInsert("conversationId", message.getConversationId())
                        .setOnInsert("bucketNo", MessageBucketDocument.bucketNo(seq)),
                MessageBucketDocument.class);
        return message;
    }

    private ChatMessage updateExisting(ChatMessage message) {
        Query q = query(where("_id").is(MessageBucketDocument.idForSeq(message.getConversationId(), message.getMessageSeq()))
                .and("messages.messageId").is(message.getId()));
        mongo.updateFirst(q, new Update()
                        .set("messages.$.content", message.getContent())
                        .set("messages.$.updatedAt", LocalDateTime.now())
                        .set("messages.$.deleted", message.isDeleted())
                        .set("messages.$.deletedAt", message.getDeletedAt())
                        .set("messages.$.deletedBy", message.getDeletedBy()),
                MessageBucketDocument.class);
        return message;
    }

    @Override
    public Optional<ChatMessage> findById(UUID messageId) {
        Query q = query(where("messages.messageId").is(messageId));
        q.fields().include("conversationId").elemMatch("messages", where("messageId").is(messageId));
        return Optional.ofNullable(mongo.findOne(q, MessageBucketDocument.class))
                .filter(b -> !b.getMessages().isEmpty())
                .map(b -> toDomain(b.getConversationId(), b.getMessages().get(0)));
    }

    @Override
    public Page<ChatMessage> findByConversationId(UUID conversationId, Pageable pageable) {
        ConversationDocument conversation = mongo.findById(conversationId, ConversationDocument.class);
        if (conversation == null) {
            return Page.empty(pageable);
        }
        List<ChatMessage> items = reader.before(conversationId, conversation.getSeq() + 1, pageable.getPageSize()).stream()
                .map(m -> toDomain(conversationId, m))
                .toList();
        return new PageImpl<>(items, pageable, items.size());
    }

    @Override
    public Optional<ChatMessage> findLatestByConversationId(UUID conversationId) {
        return findByConversationId(conversationId, Pageable.ofSize(1)).stream().findFirst();
    }

    /** No-op: {@code seen} is derived from the recipient's read watermark (migrate.md §2.3). */
    @Override
    public void markMessagesSeen(UUID conversationId, UUID viewerId, LocalDateTime readAt) {
    }

    static ChatMessage toDomain(UUID conversationId, MessageDocument m) {
        return ChatMessage.builder()
                .id(m.getMessageId())
                .conversationId(conversationId)
                .senderId(m.getSenderId())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .messageSeq(m.getSeq())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .deleted(m.isDeleted())
                .deletedAt(m.getDeletedAt())
                .deletedBy(m.getDeletedBy())
                .build();
    }
}
