package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.ReclaimLog;
import com.uit.petrescueapi.infrastructure.persistence.entity.ReclaimLogJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReclaimLogEntityMapper {
    ReclaimLog toDomain(ReclaimLogJpaEntity entity);
    ReclaimLogJpaEntity toEntity(ReclaimLog domain);
}
