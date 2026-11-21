package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.comment.CommentResponseDto;
import com.uit.petrescueapi.domain.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentWebMapper {

    @Mapping(target = "authorName", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CommentResponseDto toDto(Comment comment);
}
