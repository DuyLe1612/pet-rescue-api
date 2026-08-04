package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdoptionWebMapper {
    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "experience", source = "experience")
    @Mapping(target = "liveCondition", source = "liveCondition")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "adoptionCode", source = "adoptionCode")
    @Mapping(target = "petName", ignore = true)
    @Mapping(target = "petPrimaryImageUrl", ignore = true)
    AdoptionResponseDto toDto(AdoptionApplication app);
}
