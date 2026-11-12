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
    final com.uit.petrescueapi.application.port.out.PostReferencePort references =
            mock(com.uit.petrescueapi.application.port.out.PostReferencePort.class);
    final com.uit.petrescueapi.application.port.out.PostQueryDataPort postQuery =
            mock(com.uit.petrescueapi.application.port.out.PostQueryDataPort.class);
    final PostCommandUseCase useCase = new PostCommandUseCase(domain, accessControl, events, references, postQuery);
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

    // ── Linking a post to a rescue case ─────────────

    private static String errorCode(Throwable e) {
        return ((com.uit.petrescueapi.domain.exception.BaseException) e).getErrorCode();
    }

    @Test
    void aPostCanOnlyBeLinkedToACaseThatStillNeedsHelp() {
        UUID caseId = UUID.randomUUID();
        CreatePostRequestDto request = CreatePostRequestDto.builder().content("help").rescueCaseId(caseId).build();

        when(references.findRescueCaseStatus(caseId)).thenReturn(java.util.Optional.empty());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> useCase.create(request, author))
                .extracting(PostCommandUseCaseTest::errorCode).isEqualTo("POST_RESCUE_CASE_NOT_FOUND");

        for (String closed : List.of("RESCUED", "CLOSED")) {
            when(references.findRescueCaseStatus(caseId)).thenReturn(java.util.Optional.of(closed));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> useCase.create(request, author))
                    .extracting(PostCommandUseCaseTest::errorCode).isEqualTo("POST_RESCUE_CASE_CLOSED");
        }
        verify(domain, never()).create(any());

        when(domain.create(any())).thenAnswer(inv -> inv.getArgument(0));
        for (String open : List.of("REPORTED", "IN_PROGRESS")) {
            when(references.findRescueCaseStatus(caseId)).thenReturn(java.util.Optional.of(open));
            useCase.create(request, author);
        }
        verify(domain, times(2)).create(argThat(p -> caseId.equals(p.getRescueCaseId())));
    }

    @Test
    void anAuthorSharesACaseOnlyOnce() {
        UUID caseId = UUID.randomUUID();
        when(references.findRescueCaseStatus(caseId)).thenReturn(java.util.Optional.of("REPORTED"));
        when(postQuery.existsByAuthorAndRescueCase(author, caseId, null)).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        useCase.create(CreatePostRequestDto.builder().content("again").rescueCaseId(caseId).build(), author))
                .extracting(PostCommandUseCaseTest::errorCode).isEqualTo("POST_RESCUE_CASE_ALREADY_SHARED");
        verify(domain, never()).create(any());
    }

    @Test
    void editingAPostKeepsItsOwnLinkAndCanRemoveIt() {
        UUID caseId = UUID.randomUUID();
        when(domain.findById(postId)).thenReturn(Post.builder().postId(postId).authorId(author).build());
        when(domain.update(eq(postId), any())).thenReturn(Post.builder().postId(postId).build());
        when(references.findRescueCaseStatus(caseId)).thenReturn(java.util.Optional.of("IN_PROGRESS"));

        // the post being edited is not "another post about this case"
        useCase.update(postId, UpdatePostRequestDto.builder().rescueCaseId(caseId).build(), author);
        verify(postQuery).existsByAuthorAndRescueCase(author, caseId, postId);
        verify(domain).update(eq(postId), argThat(p -> caseId.equals(p.getRescueCaseId())));
        verify(domain, never()).unlinkRescueCase(any());

        useCase.update(postId, UpdatePostRequestDto.builder().removeRescueCase(true).build(), author);
        verify(domain).unlinkRescueCase(postId);
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
