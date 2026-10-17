package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import com.uit.petrescueapi.domain.entity.ConversationParticipant;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.config.ChatOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.ConversationDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.InboxDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.InboxDocument.PeerDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * {@link ConversationParticipantRepository} backed by the {@code inboxes} collection
 * (one inbox per participant). Read state is kept twice on purpose: {@code unread} on the inbox
 * for the list screen, and the seq watermark on the conversation to derive {@code seen}.
 */
@Component
@ChatOnMongo
@RequiredArgsConstructor
public class MongoConversationParticipantRepositoryAdapter implements ConversationParticipantRepository {

    private final MongoTemplate mongo;
    private final UserSnapshotPort userSnapshots;

    @Override
    public ConversationParticipant save(ConversationParticipant p) {
        ConversationDocument conversation = mongo.findAndModify(
                query(where("_id").is(p.getConversationId())),
                new Update().addToSet("participantIds", p.getUserId()),
                FindAndModifyOptions.options().returnNew(true),
                ConversationDocument.class);
        if (conversation == null) {
            throw new ResourceNotFoundException("Conversation", "id", p.getConversationId());
        }

        LocalDateTime sortAt = conversation.getLastMessageAt() != null ? conversation.getLastMessageAt() : p.getJoinedAt();
        mongo.upsert(query(where("_id").is(InboxDocument.idOf(p.getUserId(), p.getConversationId()))),
                new Update()
                        .set("userId", p.getUserId())
                        .set("conversationId", p.getConversationId())
                        .set("type", conversation.getType())
                        .set("name", conversation.getName())
                        .set("relatedEntityId", conversation.getRelatedEntityId())
                        .set("relatedInfo", conversation.getRelatedInfo())
                        .set("lastMessage", conversation.getLastMessage())
                        .set("unread", p.getUnreadCount())
                        .set("joinedAt", p.getJoinedAt())
                        .set("lastReadAt", p.getLastReadAt())
                        .set("sortAt", sortAt)
                        .setOnInsert("hidden", false)
                        .setOnInsert("createdAt", p.getJoinedAt()),
                InboxDocument.class);

        if (conversation.getParticipantIds().size() == 2) {
            completeDirectConversation(conversation);
        }
        return p;
    }

    /** Both participants known: set the unique direct key and each side's peer snapshot. */
    private void completeDirectConversation(ConversationDocument conversation) {
        UUID a = conversation.getParticipantIds().get(0);
        UUID b = conversation.getParticipantIds().get(1);
        mongo.updateFirst(query(where("_id").is(conversation.getId())),
                new Update().set("directKey", ConversationDocument.directKeyOf(
                        conversation.getType(), conversation.getRelatedEntityId(), a, b)),
                ConversationDocument.class);

        Map<UUID, UserSnapshot> users = userSnapshots.findByIds(List.of(a, b));
        setPeer(a, conversation.getId(), users.get(b), b);
        setPeer(b, conversation.getId(), users.get(a), a);
    }

    private void setPeer(UUID owner, UUID conversationId, UserSnapshot peer, UUID peerId) {
        PeerDocument doc = PeerDocument.builder()
                .userId(peerId)
                .name(peer == null ? null : peer.displayName())
                .avatarUrl(peer == null ? null : peer.avatarUrl())
                .build();
        mongo.updateFirst(query(where("_id").is(InboxDocument.idOf(owner, conversationId))),
                new Update().set("peer", doc), InboxDocument.class);
    }

    @Override
    public Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId) {
        return Optional.ofNullable(mongo.findById(InboxDocument.idOf(userId, conversationId), InboxDocument.class))
                .map(MongoConversationParticipantRepositoryAdapter::toDomain);
    }

    @Override
    public List<ConversationParticipant> findByConversationId(UUID conversationId) {
        return mongo.find(query(where("conversationId").is(conversationId)), InboxDocument.class).stream()
                .map(MongoConversationParticipantRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public void updateUnreadCount(UUID conversationId, UUID userId, int unreadCount, LocalDateTime lastReadAt) {
        mongo.updateFirst(query(where("_id").is(InboxDocument.idOf(userId, conversationId))),
                new Update().set("unread", unreadCount).set("lastReadAt", lastReadAt).set("hidden", false),
                InboxDocument.class);
        if (unreadCount == 0) {
            // caught up: watermark = current seq, evaluated server-side inside the same transaction
            mongo.updateFirst(query(where("_id").is(conversationId)),
                    AggregationUpdate.update().set("readSeq." + userId).toValueOf(Fields.field("seq")),
                    ConversationDocument.class);
        }
    }

    private static ConversationParticipant toDomain(InboxDocument d) {
        return ConversationParticipant.builder()
                .conversationId(d.getConversationId())
                .userId(d.getUserId())
                .joinedAt(d.getJoinedAt())
                .lastReadAt(d.getLastReadAt())
                .unreadCount(d.getUnread())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
