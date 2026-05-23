package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.entity.Conversation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatWebMapper {

    default ConversationSummaryDto toConversationDto(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        return ConversationSummaryDto.builder()
                .id(conversation.getId())
                .type(conversation.getType().name())
                .name(conversation.getName())
                .relatedInfo(conversation.getRelatedInfo())
                .relatedEntityId(conversation.getRelatedEntityId())
                .unread(0)
                .build();
    }

    default ChatMessageDto toMessageDto(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return ChatMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .time(message.getSentAt())
                .seen(message.isSeen())
                .build();
    }
}
