package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.comment.CreateCommentRequestDto;
import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.service.CommentDomainService;
import com.uit.petrescueapi.infrastructure.messaging.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentCommandUseCaseTest {

    @Test
    void createCommentPublishesCreatedEvent() {
        CommentDomainService domain = mock(CommentDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        CommentCommandUseCase useCase = new CommentCommandUseCase(domain, mock(com.uit.petrescueapi.domain.service.AccessControlDomainService.class), publisher);

        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment saved = Comment.builder()
                .commentId(UUID.randomUUID())
                .postId(postId)
                .authorId(authorId)
                .content("hello")
                .build();
        when(domain.createComment(any(Comment.class))).thenReturn(saved);

        useCase.createComment(postId, CreateCommentRequestDto.builder().content("hello").build(), authorId);

        verify(publisher).publishCommentCreated(any());
    }
}
