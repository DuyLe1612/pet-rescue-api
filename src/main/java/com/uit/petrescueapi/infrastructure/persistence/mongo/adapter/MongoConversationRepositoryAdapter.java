package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.repository.ConversationRepository;
import com.uit.petrescueapi.domain.valueobject.ConversationType;
import com.uit.petrescueapi.infrastructure.config.ChatOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.ConversationDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.InboxDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LastMessageDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * {@link ConversationRepository} on MongoDB. Saving a conversation also refreshes the
 * last-message summary of every inbox that points at it, so the conversation list needs no join.
 */
@Component
@ChatOnMongo
@RequiredArgsConstructor
public class MongoConversationRepositoryAdapter implements ConversationRepository {

    private final MongoTemplate mongo;

    @Override
    public Conversation save(Conversation c) {
        LastMessageDocument last = lastMessageOf(c);
        Update update = new Update()
                .set("type", c.getType().name())
                .set("name", c.getName())
                .set("relatedEntityId", c.getRelatedEntityId())
                .set("relatedInfo", c.getRelatedInfo())
                .set("lastMessage", last)
                .set("lastMessageAt", c.getLastMessageAt())
                .set("updatedAt", c.getUpdatedAt())
                .set("updatedBy", c.getUpdatedBy())
                .set("deleted", c.isDeleted())
                .set("deletedAt", c.getDeletedAt())
                .set("deletedBy", c.getDeletedBy())
                .setOnInsert("participantIds", new ArrayList<UUID>())
                .setOnInsert("seq", 0L)
                .setOnInsert("readSeq", new HashMap<String, Long>())
                .setOnInsert("createdAt", c.getCreatedAt())
                .setOnInsert("createdBy", c.getCreatedBy());
        mongo.upsert(query(where("_id").is(c.getId())), update, ConversationDocument.class);

        mongo.updateMulti(query(where("conversationId").is(c.getId())),
                new Update().set("lastMessage", last).set("sortAt", c.getLastMessageAt()),
                InboxDocument.class);
        return c;
    }

    @Override
    public Optional<Conversation> findById(UUID conversationId) {
        return Optional.ofNullable(mongo.findById(conversationId, ConversationDocument.class))
                .filter(d -> !d.isDeleted())
                .map(MongoConversationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<UUID> findDirectConversationId(UUID userId, UUID otherUserId, ConversationType type, UUID relatedEntityId) {
        String key = ConversationDocument.directKeyOf(type.name(), relatedEntityId, userId, otherUserId);
        return Optional.ofNullable(mongo.findOne(query(where("directKey").is(key)), ConversationDocument.class))
                .map(ConversationDocument::getId);
    }

    private static LastMessageDocument lastMessageOf(Conversation c) {
        if (c.getLastMessageSeq() == null) {
            return null;
        }
        return LastMessageDocument.builder()
                .seq(c.getLastMessageSeq())
                .senderId(c.getLastMessageSenderId())
                .preview(c.getLastMessagePreview())
                .at(c.getLastMessageAt())
                .build();
    }

    static Conversation toDomain(ConversationDocument d) {
        LastMessageDocument last = d.getLastMessage();
        return Conversation.builder()
                .id(d.getId())
                .type(ConversationType.valueOf(d.getType()))
                .name(d.getName())
                .relatedEntityId(d.getRelatedEntityId())
                .relatedInfo(d.getRelatedInfo())
                .lastMessageAt(d.getLastMessageAt())
                .lastMessagePreview(last == null ? null : last.getPreview())
                .lastMessageSenderId(last == null ? null : last.getSenderId())
                .lastMessageSeq(last == null ? null : last.getSeq())
                .createdAt(d.getCreatedAt())
                .createdBy(d.getCreatedBy())
                .updatedAt(d.getUpdatedAt())
                .updatedBy(d.getUpdatedBy())
                .deleted(d.isDeleted())
                .deletedAt(d.getDeletedAt())
                .deletedBy(d.getDeletedBy())
                .build();
    }
}
