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
    private final com.uit.petrescueapi.domain.service.AccessControlDomainService accessControl;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final com.uit.petrescueapi.application.port.out.PostReferencePort references;
    private final com.uit.petrescueapi.application.port.out.PostQueryDataPort postQuery;

    private static final java.util.Set<String> OPEN_CASE_STATUSES = java.util.Set.of("REPORTED", "IN_PROGRESS");

    @Override
    public Post create(CreatePostRequestDto cmd, UUID authorId) {
        log.debug("Command: create post by author {}", authorId);
        requireLinkableCase(cmd.getRescueCaseId(), authorId, null);
        Post post = Post.builder()
                .authorId(authorId)
                .content(cmd.getContent())
                .rescueCaseId(cmd.getRescueCaseId())
                .mediaIds(cmd.getMediaIds())
                .tagCodes(cmd.getTagCodes())
                .build();
        Post created = domainService.create(post);
        // DA2: queue the new post for AI analysis (handled after commit, off the request thread)
        events.publishEvent(new com.uit.petrescueapi.domain.event.PostContentSubmittedEvent(created.getPostId()));
        return created;
    }

    @Override
    public Post update(UUID postId, UpdatePostRequestDto cmd, UUID requesterId) {
        log.debug("Command: update post {}", postId);
        accessControl.requireSelfOrAdmin(domainService.findById(postId).getAuthorId(), requesterId);
        UUID authorId = domainService.findById(postId).getAuthorId();
        requireLinkableCase(cmd.getRescueCaseId(), authorId, postId);
        Post patch = Post.builder()
                .content(cmd.getContent())
                .mediaIds(cmd.getMediaIds())
                .tagIds(null)
                .tagCodes(cmd.getTagCodes())
                .rescueCaseId(cmd.getRescueCaseId())
                .build();
        Post updated = domainService.update(postId, patch);
        if (cmd.getRescueCaseId() == null && cmd.isRemoveRescueCase()) {
            updated = domainService.unlinkRescueCase(postId);
        }
        if (cmd.getContent() != null) {
            events.publishEvent(new com.uit.petrescueapi.domain.event.PostContentSubmittedEvent(postId));
        }
        return updated;
    }

    @Override
    public void delete(UUID postId, UUID requesterId) {
        log.debug("Command: delete post {}", postId);
        accessControl.requireSelfOrAdmin(domainService.findById(postId).getAuthorId(), requesterId);
        domainService.delete(postId);
    }

    /**
     * A post can only be attached to a case that still needs help, and each author gets one post per
     * case: sharing is for spreading the word, not for flooding the feed. Two requests at the same
     * instant can still both pass the second check; that is accepted.
     */
    private void requireLinkableCase(UUID caseId, UUID authorId, UUID postBeingEdited) {
        if (caseId == null) {
            return;
        }
        String status = references.findRescueCaseStatus(caseId).orElseThrow(() ->
                new com.uit.petrescueapi.domain.exception.BusinessException("Unknown rescue case: " + caseId, "POST_RESCUE_CASE_NOT_FOUND"));
        if (!OPEN_CASE_STATUSES.contains(status)) {
            throw new com.uit.petrescueapi.domain.exception.ConflictException(
                    "This rescue case is already closed", "POST_RESCUE_CASE_CLOSED");
        }
        if (postQuery.existsByAuthorAndRescueCase(authorId, caseId, postBeingEdited)) {
            throw new com.uit.petrescueapi.domain.exception.ConflictException(
                    "You already shared this rescue case", "POST_RESCUE_CASE_ALREADY_SHARED");
        }
    }
}
