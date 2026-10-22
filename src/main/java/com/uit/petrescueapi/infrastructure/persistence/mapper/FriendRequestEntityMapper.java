package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.FriendRequest;
import com.uit.petrescueapi.infrastructure.persistence.entity.FriendRequestJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FriendRequestEntityMapper {

    @org.mapstruct.Mapping(target = "id", source = "requestId")
    FriendRequest toDomain(FriendRequestJpaEntity entity);

    @org.mapstruct.Mapping(target = "requestId", source = "id")
    FriendRequestJpaEntity toEntity(FriendRequest domain);
}
