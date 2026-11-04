package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.comment.CreateCommentRequestDto;
import com.uit.petrescueapi.application.port.command.CommentCommandPort;
import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.service.CommentDomainService;
import com.uit.petrescueapi.infrastructure.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentCommandUseCase implements CommentCommandPort {

    private final CommentDomainService commentDomainService;
    private final com.uit.petrescueapi.domain.service.AccessControlDomainService accessControl;
    private final EventPublisher eventPublisher;

    @Override
    public Comment createComment(UUID postId, CreateCommentRequestDto cmd, UUID authorId) {
        Comment comment = Comment.builder()
                .postId(postId)
                .parentCommentId(cmd.getParentCommentId())
                .authorId(authorId)
                .content(cmd.getContent())
                .build();

        Comment saved = commentDomainService.createComment(comment);

        eventPublisher.publishCommentCreated(CommentCreatedEvent.builder()
                .commentId(saved.getCommentId())
                .postId(saved.getPostId())
                .parentCommentId(saved.getParentCommentId())
                .authorId(saved.getAuthorId())
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    @Override
    public void deleteComment(UUID commentId, UUID requesterId) {
        log.debug("Command: delete comment {}", commentId);
        accessControl.requireSelfOrAdmin(commentDomainService.findById(commentId).getAuthorId(), requesterId);
        commentDomainService.deleteComment(commentId);
    }
}
