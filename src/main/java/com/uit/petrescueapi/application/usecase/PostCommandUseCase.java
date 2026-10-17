package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.post.CreatePostRequestDto;
import com.uit.petrescueapi.application.dto.post.UpdatePostRequestDto;
import com.uit.petrescueapi.application.port.command.PostCommandPort;
import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.service.PostDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command (write) use-case for Post operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link PostDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCommandUseCase implements PostCommandPort {

    private final PostDomainService domainService;

    @Override
    public Post create(CreatePostRequestDto cmd, UUID authorId) {
        log.debug("Command: create post by author {}", authorId);
        Post post = Post.builder()
                .authorId(authorId)
                .content(cmd.getContent())
                .rescueCaseId(cmd.getRescueCaseId())
                .mediaIds(cmd.getMediaIds())
                .tagCodes(cmd.getTagCodes())
                .build();
        return domainService.create(post);
    }

    @Override
    public Post update(UUID postId, UpdatePostRequestDto cmd) {
        log.debug("Command: update post {}", postId);
        Post patch = Post.builder()
                .content(cmd.getContent())
                .mediaIds(cmd.getMediaIds())
                .tagIds(null)
                .tagCodes(cmd.getTagCodes())
                .build();
        return domainService.update(postId, patch);
    }

    @Override
    public void delete(UUID postId) {
        log.debug("Command: delete post {}", postId);
        domainService.delete(postId);
    }
}
