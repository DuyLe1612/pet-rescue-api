package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdoptionWebMapper {
    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "petId", source = "petId")
    @Mapping(target = "applicantId", source = "applicantId")
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "experience", source = "experience")
    @Mapping(target = "liveCondition", source = "liveCondition")
    @Mapping(target = "decidedAt", source = "decidedAt")
    @Mapping(target = "decidedBy", source = "decidedBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "rejectReason", source = "rejectReason")
    @Mapping(target = "readyAt", source = "readyAt")
    @Mapping(target = "petName", ignore = true)
    @Mapping(target = "petPrimaryImageUrl", ignore = true)
    @Mapping(target = "applicantUsername", ignore = true)
    @Mapping(target = "organizationName", ignore = true)
    @Mapping(target = "decidedByUsername", ignore = true)
    @Mapping(target = "adoptionCode", source = "adoptionCode")
    AdoptionResponseDto toDto(AdoptionApplication app);
}
