package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.media.MediaFileResponseDto;
import com.uit.petrescueapi.domain.entity.MediaFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaFileWebMapper {
    @Mapping(target = "mediaId", source = "mediaId")
    @Mapping(target = "uploaderId", source = "uploaderId")
    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "type", source = "resourceType")
    @Mapping(target = "createdAt", source = "createdAt")
    MediaFileResponseDto toDto(MediaFile mf);
}
