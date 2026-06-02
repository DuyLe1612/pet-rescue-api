package com.uit.petrescueapi.application.dto.admin.mapper;

import com.uit.petrescueapi.application.dto.admin.UserStatsDto;
import com.uit.petrescueapi.infrastructure.persistence.projection.UserStatsProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserStatsMapper {

    @Mapping(source = "totalUsers", target = "total")
    @Mapping(source = "activeUsers", target = "active")
    @Mapping(source = "pendingUsers", target = "pendingVerification")
    @Mapping(source = "bannedUsers", target = "banned")
    UserStatsDto toDto(UserStatsProjection projection);
}