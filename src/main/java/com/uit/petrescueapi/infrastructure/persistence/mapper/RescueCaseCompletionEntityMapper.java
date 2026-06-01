package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseCompletionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RescueCaseCompletionEntityMapper {

    RescueCaseCompletion toDomain(RescueCaseCompletionJpaEntity entity);

    @Mapping(target = "rescueCase", ignore = true)
    @Mapping(target = "verifier", ignore = true)
    RescueCaseCompletionJpaEntity toEntity(RescueCaseCompletion domain);

    List<RescueCaseCompletion> toDomainList(List<RescueCaseCompletionJpaEntity> entities);
}
