package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.domain.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostWebMapper {
    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "rescueCaseId", source = "rescueCaseId")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "authorUsername", ignore = true)
    @Mapping(target = "imageUrls", ignore = true)
    @Mapping(target = "tags", ignore = true)
    PostResponseDto toDto(Post post);
}
