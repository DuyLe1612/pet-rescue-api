package com.uit.petrescueapi.application.dto.admin.mapper;

import com.uit.petrescueapi.application.dto.admin.OrganizationStatsDto;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationStatsProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationStatsMapper {
//    @Mapping(source = "totalOrganizations",target = "total")
    OrganizationStatsDto toDto(OrganizationStatsProjection projection);
}
