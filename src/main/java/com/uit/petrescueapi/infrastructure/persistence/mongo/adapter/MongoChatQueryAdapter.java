package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.out.ChatQueryDataPort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.ConversationDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.InboxDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.MessageBucketDocument.MessageDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Chat read side on MongoDB: the conversation list is one indexed range scan over
 * {@code inboxes}; message history reads 1–2 buckets per page. No joins, no cache.
 */
@Component
@RequiredArgsConstructor
public class MongoChatQueryAdapter implements ChatQueryDataPort {

    private final MongoTemplate mongo;
    private final MessageBucketReader reader;

    @Override
    public Page<ConversationSummaryDto> listConversationsByCursor(UUID userId, LocalDateTime cursor, Pageable pageable) {
        Criteria criteria = where("userId").is(userId).and("hidden").is(false);
        if (cursor != null) {
            criteria = criteria.and("sortAt").lt(cursor);
        }
        Query q = query(criteria)
                .with(Sort.by(Sort.Order.desc("sortAt"), Sort.Order.desc("conversationId")))
                .limit(pageable.getPageSize());
        List<ConversationSummaryDto> items = mongo.find(q, InboxDocument.class).stream()
                .map(MongoChatQueryAdapter::toSummary)
                .toList();
        return new PageImpl<>(items, pageable, items.size());
    }

    @Override
    public Page<ChatMessageDto> listMessagesByCursor(UUID conversationId, UUID userId, LocalDateTime cursor,
                                                     Long cursorSeq, String direction, Pageable pageable) {
        if (!mongo.exists(query(where("_id").is(InboxDocument.idOf(userId, conversationId))), InboxDocument.class)) {
            throw new BusinessException("User not in conversation", "CHAT_FORBIDDEN");
        }
        ConversationDocument conversation = mongo.findById(conversationId, ConversationDocument.class);
        if (conversation == null) {
            throw new ResourceNotFoundException("Conversation", "id", conversationId);
        }

        int size = pageable.getPageSize();
        boolean isAfter = "after".equalsIgnoreCase(direction);
        List<MessageDocument> messages = (isAfter && cursorSeq != null)
                ? reader.after(conversationId, cursorSeq, conversation.getSeq(), size)
                : reader.before(conversationId, cursorSeq != null ? cursorSeq : conversation.getSeq() + 1, size);

        List<ChatMessageDto> items = messages.stream()
                .map(m -> ChatMessageDto.builder()
                        .id(m.getMessageId())
                        .senderId(m.getSenderId())
                        .content(m.getContent())
                        .time(m.getSentAt())
                        .messageSeq(m.getSeq())
                        .seen(m.getSeq() <= recipientReadSeq(conversation, m.getSenderId()))
                        .build())
                .toList();
        return new PageImpl<>(items, pageable, items.size());
    }

    /** {@code seen} = the other participant's watermark has passed this message. */
    private static long recipientReadSeq(ConversationDocument conversation, UUID senderId) {
        return conversation.getParticipantIds().stream()
                .filter(id -> !id.equals(senderId))
                .mapToLong(conversation::readSeqOf)
                .max()
                .orElse(0L);
    }

    private static ConversationSummaryDto toSummary(InboxDocument d) {
        InboxDocument.PeerDocument peer = d.getPeer();
        String peerName = peer == null ? null : peer.getName();
        return ConversationSummaryDto.builder()
                .id(d.getConversationId())
                .type(d.getType())
                .name(peerName != null ? peerName : d.getName())
                .otherUserId(peer == null ? null : peer.getUserId())
                .otherUserName(peerName)
                .otherUserAvatarUrl(peer == null ? null : peer.getAvatarUrl())
                .lastMessage(d.getLastMessage() == null ? null : d.getLastMessage().getPreview())
                .lastTime(d.getSortAt())
                .unread(d.getUnread())
                .relatedInfo(d.getRelatedInfo())
                .relatedEntityId(d.getRelatedEntityId())
                .build();
    }
}
