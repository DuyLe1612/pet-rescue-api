package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service encapsulating Post business rules.
 *
 * Rules:
 *  - New posts get a generated id and createdAt timestamp.
 *  - Deletion is soft-delete (sets deleted = true).
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
// chat/post data lives in MongoDB; a plain @Transactional would open a JPA transaction (MongoConfig)
@Transactional(transactionManager = "mongoTransactionManager")
public class PostDomainService {

    private final PostRepository postRepository;

    // ── Queries ─────────────────────────────────────

    @Transactional(transactionManager = "mongoTransactionManager", readOnly = true)
    public Post findById(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));
    }

    // ── Commands ────────────────────────────────────

    /**
     * Create a new post.
     * Sets the id and createdAt timestamp.
     */
    public Post create(Post post) {
        log.info("Creating new post by author {}", post.getAuthorId());
        post.setPostId(UUID.randomUUID());
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    /**
     * Partial update of an existing post.
     */
    public Post update(UUID postId, Post patch) {
        log.debug("Updating post {}", postId);
        Post existing = findById(postId);
        applyUpdates(existing, patch);
        existing.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(existing);
    }

    /**
     * Soft delete a post.
     */
    public void delete(UUID postId) {
        Post post = findById(postId);
        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
        log.info("Soft-deleted post {}", postId);
    }

    // ── Private helpers ─────────────────────────────

    private void applyUpdates(Post target, Post source) {
        if (source.getContent() != null)      target.setContent(source.getContent());
        if (source.getMediaIds() != null)     target.setMediaIds(source.getMediaIds());
        if (source.getTagIds() != null)       target.setTagIds(source.getTagIds());
        if (source.getTagCodes() != null)     target.setTagCodes(source.getTagCodes());
        if (source.getRescueCaseId() != null) target.setRescueCaseId(source.getRescueCaseId());
    }
}
