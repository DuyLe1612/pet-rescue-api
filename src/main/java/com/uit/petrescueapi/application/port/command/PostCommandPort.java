package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.post.CreatePostRequestDto;
import com.uit.petrescueapi.application.dto.post.UpdatePostRequestDto;
import com.uit.petrescueapi.domain.entity.Post;

import java.util.UUID;

public interface PostCommandPort {
    Post create(CreatePostRequestDto cmd, UUID authorId);
    Post update(UUID postId, UpdatePostRequestDto cmd, UUID requesterId);
    void delete(UUID postId, UUID requesterId);
}
