package com.uit.petrescueapi.application.dto.admin.mapper;

import com.uit.petrescueapi.application.dto.admin.DashboardOverviewDto;
import com.uit.petrescueapi.application.dto.admin.mapper.OrganizationStatsMapper;
import com.uit.petrescueapi.application.dto.admin.mapper.PetStatsMapper;
import com.uit.petrescueapi.application.dto.admin.mapper.UserStatsMapper;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationStatsProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetStatsProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.UserStatsProjection;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = {
                UserStatsMapper.class,
                PetStatsMapper.class,
                OrganizationStatsMapper.class
        }
)
public interface DashboardMapper {

    DashboardOverviewDto toDto(
            UserStatsProjection users,
            PetStatsProjection pets,
            OrganizationStatsProjection organizations
    );
}