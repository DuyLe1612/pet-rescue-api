package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.post.CreatePostRequestDto;
import com.uit.petrescueapi.application.dto.post.UpdatePostRequestDto;
import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.event.PostContentSubmittedEvent;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.PostDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostCommandUseCaseTest {

    final PostDomainService domain = mock(PostDomainService.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final AccessControlDomainService accessControl = mock(AccessControlDomainService.class);
    final PostCommandUseCase useCase = new PostCommandUseCase(domain, accessControl, events);
    final UUID postId = UUID.randomUUID();
    final UUID author = UUID.randomUUID();

    @Test
    void createPassesTagCodesAndQueuesAnalysis() {
        when(domain.create(any())).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setPostId(postId);
            return p;
        });

        useCase.create(CreatePostRequestDto.builder().content("hi").tagCodes(List.of("dog")).build(), UUID.randomUUID());

        verify(domain).create(argThat(p -> p.getTagCodes().equals(List.of("dog"))));
        verify(events).publishEvent(new PostContentSubmittedEvent(postId));
    }

    @Test
    void contentEditQueuesAnalysisButMediaOnlyEditDoesNot() {
        when(domain.findById(postId)).thenReturn(Post.builder().postId(postId).authorId(author).build());
        when(domain.update(eq(postId), any())).thenReturn(Post.builder().postId(postId).build());

        useCase.update(postId, UpdatePostRequestDto.builder().mediaIds(List.of()).build(), author);
        verifyNoInteractions(events);

        useCase.update(postId, UpdatePostRequestDto.builder().content("new text").build(), author);
        verify(events).publishEvent(new PostContentSubmittedEvent(postId));
    }

    @Test
    void nonAuthorCannotEditOrDelete() {
        UUID stranger = UUID.randomUUID();
        when(domain.findById(postId)).thenReturn(Post.builder().postId(postId).authorId(author).build());
        doThrow(new ForbiddenException("no")).when(accessControl).requireSelfOrAdmin(author, stranger);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> useCase.update(postId, UpdatePostRequestDto.builder().content("x").build(), stranger))
                .isInstanceOf(ForbiddenException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> useCase.delete(postId, stranger))
                .isInstanceOf(ForbiddenException.class);
        verify(domain, never()).update(any(), any());
        verify(domain, never()).delete(any());
    }
}
