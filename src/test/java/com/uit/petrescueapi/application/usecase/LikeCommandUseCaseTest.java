package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.domain.service.LikeDomainService;
import com.uit.petrescueapi.infrastructure.messaging.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LikeCommandUseCaseTest {

    @Test
    void likePostPublishesEventWhenStateChanged() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.likePost(postId, userId)).thenReturn(true);

        var result = useCase.likePost(postId, userId);

        verify(publisher).publishPostLiked(any());
        assertThat(result.isLiked()).isTrue();
        assertThat(result.getTargetId()).isEqualTo(postId);
    }

    @Test
    void likePostDoesNotPublishWhenDuplicate() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.likePost(postId, userId)).thenReturn(false);

        var result = useCase.likePost(postId, userId);

        verify(publisher, never()).publishPostLiked(any());
        assertThat(result.isLiked()).isTrue();
        assertThat(result.getTargetId()).isEqualTo(postId);
    }

    @Test
    void unlikePostPublishesEventWhenStateChanged() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.unlikePost(postId, userId)).thenReturn(true);

        var result = useCase.unlikePost(postId, userId);

        verify(publisher).publishPostUnliked(any());
        assertThat(result.isLiked()).isFalse();
        assertThat(result.getTargetId()).isEqualTo(postId);
    }

    @Test
    void unlikePostDoesNotPublishWhenNothingToRemove() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.unlikePost(postId, userId)).thenReturn(false);

        var result = useCase.unlikePost(postId, userId);

        verify(publisher, never()).publishPostUnliked(any());
        assertThat(result.isLiked()).isFalse();
        assertThat(result.getTargetId()).isEqualTo(postId);
    }

    @Test
    void likeCommentPublishesEventWhenStateChanged() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.likeComment(commentId, userId)).thenReturn(true);

        var result = useCase.likeComment(commentId, userId);

        verify(publisher).publishCommentLiked(any());
        assertThat(result.isLiked()).isTrue();
        assertThat(result.getTargetId()).isEqualTo(commentId);
    }

    @Test
    void likeCommentDoesNotPublishWhenDuplicate() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.likeComment(commentId, userId)).thenReturn(false);

        var result = useCase.likeComment(commentId, userId);

        verify(publisher, never()).publishCommentLiked(any());
        assertThat(result.isLiked()).isTrue();
        assertThat(result.getTargetId()).isEqualTo(commentId);
    }

    @Test
    void unlikeCommentPublishesEventWhenStateChanged() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.unlikeComment(commentId, userId)).thenReturn(true);

        var result = useCase.unlikeComment(commentId, userId);

        verify(publisher).publishCommentUnliked(any());
        assertThat(result.isLiked()).isFalse();
        assertThat(result.getTargetId()).isEqualTo(commentId);
    }

    @Test
    void unlikeCommentDoesNotPublishWhenNothingToRemove() {
        LikeDomainService domain = mock(LikeDomainService.class);
        EventPublisher publisher = mock(EventPublisher.class);
        LikeCommandUseCase useCase = new LikeCommandUseCase(domain, publisher);

        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(domain.unlikeComment(commentId, userId)).thenReturn(false);

        var result = useCase.unlikeComment(commentId, userId);

        verify(publisher, never()).publishCommentUnliked(any());
        assertThat(result.isLiked()).isFalse();
        assertThat(result.getTargetId()).isEqualTo(commentId);
    }
}
