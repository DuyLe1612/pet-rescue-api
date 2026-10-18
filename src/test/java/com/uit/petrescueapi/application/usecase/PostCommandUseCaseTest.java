package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.post.CreatePostRequestDto;
import com.uit.petrescueapi.application.dto.post.UpdatePostRequestDto;
import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.event.PostContentSubmittedEvent;
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
    final PostCommandUseCase useCase = new PostCommandUseCase(domain, events);
    final UUID postId = UUID.randomUUID();

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
        when(domain.update(eq(postId), any())).thenReturn(Post.builder().postId(postId).build());

        useCase.update(postId, UpdatePostRequestDto.builder().mediaIds(List.of()).build());
        verifyNoInteractions(events);

        useCase.update(postId, UpdatePostRequestDto.builder().content("new text").build());
        verify(events).publishEvent(new PostContentSubmittedEvent(postId));
    }
}
