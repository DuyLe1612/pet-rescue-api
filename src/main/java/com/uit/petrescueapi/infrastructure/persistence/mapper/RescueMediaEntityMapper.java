package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.RescueMedia;
import com.uit.petrescueapi.infrastructure.persistence.entity.RescueMediaJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RescueMediaEntityMapper {

    RescueMedia toDomain(RescueMediaJpaEntity entity);

    RescueMediaJpaEntity toEntity(RescueMedia domain);

    List<RescueMedia> toDomainList(
            List<RescueMediaJpaEntity> entities
    );
}
