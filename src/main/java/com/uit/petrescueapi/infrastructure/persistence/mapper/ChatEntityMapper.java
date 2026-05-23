package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.ChatMessage;
import com.uit.petrescueapi.domain.entity.Conversation;
import com.uit.petrescueapi.domain.entity.ConversationParticipant;
import com.uit.petrescueapi.domain.entity.FriendRequest;
import com.uit.petrescueapi.infrastructure.persistence.entity.ChatMessageJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.ConversationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.ConversationParticipantJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.FriendRequestJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatEntityMapper {

    @org.mapstruct.Mapping(target = "id", source = "conversationId")
    Conversation toDomain(ConversationJpaEntity entity);

    @org.mapstruct.Mapping(target = "conversationId", source = "id")
    ConversationJpaEntity toEntity(Conversation domain);

    @org.mapstruct.Mapping(target = "id", source = "messageId")
    ChatMessage toDomain(ChatMessageJpaEntity entity);

    @org.mapstruct.Mapping(target = "messageId", source = "id")
    ChatMessageJpaEntity toEntity(ChatMessage domain);

    ConversationParticipant toDomain(ConversationParticipantJpaEntity entity);

    ConversationParticipantJpaEntity toEntity(ConversationParticipant domain);

    @org.mapstruct.Mapping(target = "id", source = "requestId")
    FriendRequest toDomain(FriendRequestJpaEntity entity);

    @org.mapstruct.Mapping(target = "requestId", source = "id")
    FriendRequestJpaEntity toEntity(FriendRequest domain);

    List<ConversationParticipant> toDomainParticipants(List<ConversationParticipantJpaEntity> entities);

    List<ChatMessage> toDomainMessages(List<ChatMessageJpaEntity> entities);
}
