package com.uit.petrescueapi.application.dto.admin.mapper;

import com.uit.petrescueapi.application.dto.admin.PetStatsDto;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetStatsProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PetStatsMapper {
//    @Mapping(source = "totalPets", target = "total")
//    @Mapping(source = "availablePets", target = "available")
//    @Mapping(source = "adoptedPets",target = "adopted")
//    @Mapping(source = "pendingPets", target = "pending")
    PetStatsDto toDto(PetStatsProjection projection);
}
