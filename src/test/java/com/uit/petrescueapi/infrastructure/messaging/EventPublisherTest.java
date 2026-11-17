package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.event.CommentLikedEvent;
import com.uit.petrescueapi.domain.event.CommentUnlikedEvent;
import com.uit.petrescueapi.domain.event.PostLikedEvent;
import com.uit.petrescueapi.domain.event.PostUnlikedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private PostLikeEventListener postLikeHandler;
    private CommentEventListener commentHandler;
    private EventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        postLikeHandler = mock(PostLikeEventListener.class);
        commentHandler = mock(CommentEventListener.class);
        publisher = new EventPublisher(rabbitTemplate, postLikeHandler, commentHandler);
        ReflectionTestUtils.setField(publisher, "socialExchange", "social.exchange");
        ReflectionTestUtils.setField(publisher, "postLikedRoutingKey", "post.liked");
        ReflectionTestUtils.setField(publisher, "postUnlikedRoutingKey", "post.unliked");
        ReflectionTestUtils.setField(publisher, "commentCreatedRoutingKey", "comment.created");
        ReflectionTestUtils.setField(publisher, "commentLikedRoutingKey", "comment.liked");
        ReflectionTestUtils.setField(publisher, "commentUnlikedRoutingKey", "comment.unliked");
    }

    @Test
    void publishPostLikedSendsToExpectedRoutingKey() {
        PostLikedEvent event = PostLikedEvent.builder()
                .postId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build();
        publisher.publishPostLiked(event);
        verify(rabbitTemplate).convertAndSend("social.exchange", "post.liked", event);
    }

    @Test
    void publishPostUnlikedSendsToExpectedRoutingKey() {
        PostUnlikedEvent event = PostUnlikedEvent.builder()
                .postId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build();
        publisher.publishPostUnliked(event);
        verify(rabbitTemplate).convertAndSend("social.exchange", "post.unliked", event);
    }

    @Test
    void publishCommentCreatedSendsToExpectedRoutingKey() {
        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .commentId(UUID.randomUUID())
                .postId(UUID.randomUUID())
                .parentCommentId(null)
                .authorId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build();
        publisher.publishCommentCreated(event);
        verify(rabbitTemplate).convertAndSend("social.exchange", "comment.created", event);
    }

    @Test
    void publishCommentLikedSendsToExpectedRoutingKey() {
        CommentLikedEvent event = CommentLikedEvent.builder()
                .commentId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build();
        publisher.publishCommentLiked(event);
        verify(rabbitTemplate).convertAndSend("social.exchange", "comment.liked", event);
    }

    @Test
    void publishCommentUnlikedSendsToExpectedRoutingKey() {
        CommentUnlikedEvent event = CommentUnlikedEvent.builder()
                .commentId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .build();
        publisher.publishCommentUnliked(event);
        verify(rabbitTemplate).convertAndSend("social.exchange", "comment.unliked", event);
    }

    // ── the broker is down ──────────────────────────

    private void brokerIsDown() {
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void aQueuedEventNeverTouchesTheDirectPath() {
        publisher.publishPostLiked(PostLikedEvent.builder().postId(UUID.randomUUID()).userId(UUID.randomUUID()).build());

        verifyNoInteractions(postLikeHandler, commentHandler);
    }

    @Test
    void withoutABrokerEveryCounterChangeIsAppliedDirectlyAndNothingIsThrown() {
        brokerIsDown();
        PostLikedEvent liked = PostLikedEvent.builder().postId(UUID.randomUUID()).userId(UUID.randomUUID()).build();
        PostUnlikedEvent unliked = PostUnlikedEvent.builder().postId(UUID.randomUUID()).userId(UUID.randomUUID()).build();
        CommentCreatedEvent created = CommentCreatedEvent.builder().commentId(UUID.randomUUID()).postId(UUID.randomUUID()).build();
        CommentLikedEvent commentLiked = CommentLikedEvent.builder().commentId(UUID.randomUUID()).userId(UUID.randomUUID()).build();
        CommentUnlikedEvent commentUnliked = CommentUnlikedEvent.builder().commentId(UUID.randomUUID()).userId(UUID.randomUUID()).build();

        assertThatCode(() -> {
            publisher.publishPostLiked(liked);
            publisher.publishPostUnliked(unliked);
            publisher.publishCommentCreated(created);
            publisher.publishCommentLiked(commentLiked);
            publisher.publishCommentUnliked(commentUnliked);
        }).doesNotThrowAnyException();

        verify(postLikeHandler).handlePostLiked(liked);
        verify(postLikeHandler).handlePostUnliked(unliked);
        verify(commentHandler).handleCommentCreated(created);
        verify(commentHandler).handleCommentLiked(commentLiked);
        verify(commentHandler).handleCommentUnliked(commentUnliked);
    }

    @Test
    void aBrokerAndACounterStoreBothDownStillDoNotFailTheRequest() {
        brokerIsDown();
        CommentCreatedEvent created = CommentCreatedEvent.builder().commentId(UUID.randomUUID()).postId(UUID.randomUUID()).build();
        doThrow(new IllegalStateException("redis down")).when(commentHandler).handleCommentCreated(created);

        assertThatCode(() -> publisher.publishCommentCreated(created)).doesNotThrowAnyException();
    }
}
