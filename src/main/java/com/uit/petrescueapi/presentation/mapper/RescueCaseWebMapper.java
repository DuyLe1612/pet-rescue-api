package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.rescue.RescueCaseCompletionResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseResponseDto;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RescueCaseWebMapper {
    @Mapping(target = "caseId", source = "caseId")
    @Mapping(target = "status", expression = "java(rc.getStatus() != null ? rc.getStatus().name() : null)")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "reportedAt", source = "reportedAt")
    @Mapping(target = "reporterUsername", ignore = true)
    @Mapping(target = "imageUrls", source = "imagePublicIds")
    @Mapping(target = "contactPhone", source = "contactPhone")
    RescueCaseResponseDto toDto(RescueCase rc);

    RescueCaseCompletionResponseDto toDto(RescueCaseCompletion rc);
}
