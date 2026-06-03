package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.RescueCompletionMedia;
import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCompletionMediaJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RescueCompletionMediaEntityMapper {

    @Mapping(target = "completionId", source = "id.completionId")
    @Mapping(target = "mediaId", source = "id.mediaId")
    RescueCompletionMedia toDomain(
            RescueCompletionMediaJpaEntity entity
    );

    @Mapping(target = "id.completionId", source = "completionId")
    @Mapping(target = "id.mediaId", source = "mediaId")
    @Mapping(target = "completion", ignore = true)
    @Mapping(target = "media", ignore = true)
    RescueCompletionMediaJpaEntity toEntity(
            RescueCompletionMedia domain
    );

    List<RescueCompletionMedia> toDomainList(
            List<RescueCompletionMediaJpaEntity> entities
    );
}
