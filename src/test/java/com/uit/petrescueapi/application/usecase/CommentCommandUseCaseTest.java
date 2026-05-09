package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.comment.CreateCommentRequestDto;
import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.service.CommentDomainService;
import com.uit.petrescueapi.infrastructure.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentCommandUseCaseTest {

    private CommentDomainService domain;
    private EventPublisher publisher;
    private CommentCommandUseCase useCase;

    @BeforeEach
    void setUp() {
        domain = mock(CommentDomainService.class);
        publisher = mock(EventPublisher.class);
        useCase = new CommentCommandUseCase(domain, publisher);
    }

    @Test
    void createCommentPublishesCreatedEvent() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID parentCommentId = UUID.randomUUID();
        CreateCommentRequestDto request = CreateCommentRequestDto.builder()
                .content("hello")
                .parentCommentId(parentCommentId)
                .build();
        Comment saved = Comment.builder()
                .commentId(UUID.randomUUID())
                .postId(postId)
                .parentCommentId(parentCommentId)
                .authorId(authorId)
                .content("hello")
                .build();
        when(domain.createComment(any(Comment.class))).thenReturn(saved);

        Comment result = useCase.createComment(postId, request, authorId);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(domain).createComment(commentCaptor.capture());
        Comment toCreate = commentCaptor.getValue();
        assertThat(toCreate.getPostId()).isEqualTo(postId);
        assertThat(toCreate.getParentCommentId()).isEqualTo(parentCommentId);
        assertThat(toCreate.getAuthorId()).isEqualTo(authorId);
        assertThat(toCreate.getContent()).isEqualTo("hello");

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(publisher).publishCommentCreated(eventCaptor.capture());
        CommentCreatedEvent event = eventCaptor.getValue();
        assertThat(event.getCommentId()).isEqualTo(saved.getCommentId());
        assertThat(event.getPostId()).isEqualTo(saved.getPostId());
        assertThat(event.getParentCommentId()).isEqualTo(saved.getParentCommentId());
        assertThat(event.getAuthorId()).isEqualTo(saved.getAuthorId());
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void deleteCommentDelegatesToDomainService() {
        UUID commentId = UUID.randomUUID();

        useCase.deleteComment(commentId);

        verify(domain).deleteComment(commentId);
        verifyNoInteractions(publisher);
    }
}
