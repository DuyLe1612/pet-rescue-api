package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.entity.ConversationParticipant;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.ChatMessageRepository;
import com.uit.petrescueapi.domain.repository.ConversationParticipantRepository;
import com.uit.petrescueapi.domain.repository.ConversationRepository;
import com.uit.petrescueapi.domain.repository.UserRepository;
import com.uit.petrescueapi.domain.valueobject.ConversationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatDomainService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    public Conversation createDirectConversation(UUID requesterId,
                                                 UUID participantId,
                                                 ConversationType type,
                                                 UUID relatedEntityId,
                                                 String name,
                                                 String relatedInfo) {
        if (requesterId.equals(participantId)) {
            throw new BusinessException("Cannot create conversation with yourself", "CHAT_SELF");
        }

        userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", requesterId));
        userRepository.findById(participantId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", participantId));

        Conversation existingConversation = conversationRepository
                .findDirectConversationId(requesterId, participantId, type, relatedEntityId)
                .flatMap(conversationRepository::findById)
                .orElse(null);
        if (existingConversation != null) {
            return existingConversation;
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .type(type)
                .name(name)
                .relatedEntityId(relatedEntityId)
                .relatedInfo(relatedInfo)
            .lastMessageAt(now)
            .lastMessagePreview(null)
            .lastMessageSenderId(null)
            .lastMessageSeq(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Conversation saved = conversationRepository.save(conversation);

        participantRepository.save(ConversationParticipant.builder()
                .conversationId(saved.getId())
                .userId(requesterId)
                .joinedAt(now)
                .lastReadAt(now)
                .unreadCount(0)
                .build());

        participantRepository.save(ConversationParticipant.builder()
                .conversationId(saved.getId())
                .userId(participantId)
                .joinedAt(now)
                .lastReadAt(null)
                .unreadCount(0)
                .build());

        log.debug("Created conversation {} between {} and {}", saved.getId(), requesterId, participantId);
        return saved;
    }

    public ChatMessage sendMessage(UUID conversationId, UUID senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        ConversationParticipant sender = participantRepository.findByConversationIdAndUserId(conversationId, senderId)
                .orElseThrow(() -> new BusinessException("Sender not in conversation", "CHAT_FORBIDDEN"));

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversation.getId())
                .senderId(senderId)
                .content(content)
                .sentAt(now)
                .seen(false)
                .createdAt(now)
                .build();

        ChatMessage saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getSentAt());
        conversation.setLastMessagePreview(toPreview(saved.getContent()));
        conversation.setLastMessageSenderId(saved.getSenderId());
        conversation.setLastMessageSeq(saved.getMessageSeq());
        conversationRepository.save(conversation);

        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        for (ConversationParticipant participant : participants) {
            if (participant.getUserId().equals(senderId)) {
                participantRepository.updateUnreadCount(conversationId, senderId, 0, now);
            } else {
                int newUnread = Math.max(0, participant.getUnreadCount()) + 1;
                participantRepository.updateUnreadCount(conversationId, participant.getUserId(), newUnread, participant.getLastReadAt());
            }
        }

        return saved;
    }

    public void refreshLastMessage(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        ChatMessage latest = messageRepository.findLatestByConversationId(conversationId).orElse(null);
        if (latest == null) {
            conversation.setLastMessageAt(conversation.getCreatedAt());
            conversation.setLastMessagePreview(null);
            conversation.setLastMessageSenderId(null);
            conversation.setLastMessageSeq(null);
        } else {
            conversation.setLastMessageAt(latest.getSentAt());
            conversation.setLastMessagePreview(toPreview(latest.getContent()));
            conversation.setLastMessageSenderId(latest.getSenderId());
            conversation.setLastMessageSeq(latest.getMessageSeq());
        }
        conversationRepository.save(conversation);
    }

    private String toPreview(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.length() <= 200) {
            return trimmed;
        }
        return trimmed.substring(0, 200);
    }

    public void markRead(UUID conversationId, UUID userId) {
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException("User not in conversation", "CHAT_FORBIDDEN"));
        LocalDateTime now = LocalDateTime.now();
        participantRepository.updateUnreadCount(conversationId, userId, 0, now);
        messageRepository.markMessagesSeen(conversationId, userId, now);
    }

    public Conversation deleteMessage(UUID conversationId, UUID messageId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatMessage", "id", messageId));

        if (!conversationId.equals(message.getConversationId())) {
            throw new BusinessException("Message does not belong to this conversation", "CHAT_INVALID_MESSAGE");
        }
        if (!userId.equals(message.getSenderId())) {
            throw new BusinessException("Not allowed to delete this message", "CHAT_FORBIDDEN");
        }

        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setDeletedBy(userId);
        messageRepository.save(message);

        if (conversation.getLastMessageSeq() != null
                && conversation.getLastMessageSeq().equals(message.getMessageSeq())) {
            refreshLastMessage(conversationId);
            return conversationRepository.findById(conversationId).orElse(conversation);
        }

        return conversation;
    }
}
