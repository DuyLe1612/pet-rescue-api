package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.entity.PostJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.TagJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.PostDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.PostSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Query-side adapter (CQRS read path) for Post.
 *
 * <p>Executes optimized JOIN queries via {@link PostQueryJpaRepository},
 * maps infrastructure projections to application DTOs.</p>
 *
 * <p>For detail view, also loads tags and media from {@link PostJpaRepository}
 * via entity relationships.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryAdapter implements PostQueryDataPort {

    private final PostQueryJpaRepository queryRepo;
    private final PostJpaRepository postJpaRepo;
        private final CloudStoragePort cloudStoragePort;

    // ── List (summary) queries ──────────────────

    @Override
    public Page<PostSummaryResponseDto> findAllSummaries(Pageable pageable) {
        return queryRepo.findAllSummaries(pageable).map(this::toSummaryDto);
    }

    @Override
    public PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, int size, UUID viewerId) {
        Page<PostSummaryResponseDto> page = (cursor == null)
                ? queryRepo.findFirstFeedPage( PageRequest.of(0, size)).map(this::toSummaryDto)
                : queryRepo.findFeedByCursor(cursor, viewerId, PageRequest.of(0, size)).map(this::toSummaryDto);

        List<PostSummaryResponseDto> items = page.getContent();

        // Batch-load media for all posts in the page to avoid N+1 queries
        List<UUID> postIds = items.stream().map(PostSummaryResponseDto::getPostId).toList();
        if (!postIds.isEmpty()) {
            var entities = postJpaRepo.findAllById(postIds);
            var mediaMap = entities.stream().collect(
                    java.util.stream.Collectors.toMap(
                            e -> e.getPostId(),
                            e -> e.getMediaFiles().stream()
                                    .map(m -> cloudStoragePort.buildUrl(m.getPublicId()))
                                    .toList()
                    )
            );

            for (PostSummaryResponseDto item : items) {
                item.setImageUrls(mediaMap.getOrDefault(item.getPostId(), java.util.Collections.emptyList()));
            }
        }
        LocalDateTime nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getCreatedAt();
        boolean hasMore = items.size() == size;

        return PostCursorResponseDto.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    // ── Detail (single post) query ──────────────

    @Override
    public PostResponseDto findById(UUID postId) {
        PostDetailProjection proj = queryRepo.findDetailById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));

        // Load entity to get tags and media (lazy collections)
        PostJpaEntity entity = postJpaRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));

        List<String> tags = entity.getTags().stream()
                .map(TagJpaEntity::getCode)
                .toList();

        List<String> mediaUrls = entity.getMediaFiles().stream()
                .map(m -> cloudStoragePort.buildUrl(m.getPublicId()))
                .toList();

        return toResponseDto(proj, tags, mediaUrls);
    }

    // ── Projection → DTO mappers ────────────────

    private PostSummaryResponseDto toSummaryDto(PostSummaryProjection p) {
        return PostSummaryResponseDto.builder()
                .postId(p.getPostId())
                .authorId(p.getAuthorId())
                .authorUsername(p.getAuthorUsername())
                .content(p.getContent())
                .tags(Collections.emptyList())
                .likeCount(p.getLikeCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .build();
    }

        private PostResponseDto toResponseDto(PostDetailProjection p,
                                                                                  List<String> tags,
                                                                                  List<String> imageUrls) {
        return PostResponseDto.builder()
                .postId(p.getPostId())
                .authorId(p.getAuthorId())
                .authorUsername(p.getAuthorUsername())
                .rescueCaseId(p.getRescueCaseId())
                .content(p.getContent())
                                .imageUrls(imageUrls)
                .tags(tags)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
