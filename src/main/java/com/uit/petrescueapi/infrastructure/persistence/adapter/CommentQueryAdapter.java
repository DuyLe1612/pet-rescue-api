package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.comment.CommentResponseDto;
import com.uit.petrescueapi.application.dto.comment.CommentSummaryDto;
import com.uit.petrescueapi.application.port.out.CommentQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.projection.CommentDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.CommentSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.CommentQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryAdapter implements CommentQueryDataPort {

    private final CommentQueryJpaRepository queryRepo;

    @Override
    public Page<CommentSummaryDto> findParentCommentsByPostId(UUID postId, LocalDateTime cursor, Pageable pageable) {
        Page<CommentSummaryProjection> page = cursor == null
                ? queryRepo.findParentCommentsByPostId(postId, pageable)
                : queryRepo.findParentCommentsByPostIdBeforeCursor(postId, cursor, pageable);
        return page.map(this::toSummaryDto);
    }

    @Override
    public Page<CommentSummaryDto> findRepliesByParentCommentId(UUID parentCommentId, Pageable pageable) {
        return queryRepo.findRepliesByParentCommentId(parentCommentId, pageable).map(this::toSummaryDto);
    }

    @Override
    public CommentResponseDto findById(UUID commentId) {
        CommentDetailProjection projection = queryRepo.findDetailById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "commentId", commentId));
        return toResponseDto(projection);
    }

    private CommentSummaryDto toSummaryDto(CommentSummaryProjection p) {
        return CommentSummaryDto.builder()
                .commentId(p.getCommentId())
                .postId(p.getPostId())
                .parentCommentId(p.getParentCommentId())
                .authorId(p.getAuthorId())
                .authorUsername(p.getAuthorUsername())
                .content(p.getContent())
                .likeCount(p.getLikeCount())
                .replyCount(p.getReplyCount())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private CommentResponseDto toResponseDto(CommentDetailProjection p) {
        return CommentResponseDto.builder()
                .commentId(p.getCommentId())
                .postId(p.getPostId())
                .parentCommentId(p.getParentCommentId())
                .authorId(p.getAuthorId())
                .authorUsername(p.getAuthorUsername())
                .content(p.getContent())
                .likeCount(p.getLikeCount())
                .replyCount(p.getReplyCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
