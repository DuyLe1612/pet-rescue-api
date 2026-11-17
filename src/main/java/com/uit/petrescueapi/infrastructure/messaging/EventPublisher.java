package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.event.CommentLikedEvent;
import com.uit.petrescueapi.domain.event.CommentUnlikedEvent;
import com.uit.petrescueapi.domain.event.PostLikedEvent;
import com.uit.petrescueapi.domain.event.PostUnlikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Hands the social counter events to RabbitMQ. A like or a comment is already saved when this is
 * called, so the broker being down must never turn a successful write into an error: the counter
 * change is then applied directly, by the very handler the queue would have fed, and if even that
 * fails the problem is logged and the request still succeeds.
 *
 * <p>Applying directly is safe to repeat for likes (Redis de-duplicates them). A comment count has
 * no such guard, but this path only runs when nothing was queued.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final PostLikeEventListener postLikeHandler;
    private final CommentEventListener commentHandler;

    @Value("${app.rabbitmq.exchange.social}")
    private String socialExchange;

    @Value("${app.rabbitmq.routing-key.post-liked}")
    private String postLikedRoutingKey;

    @Value("${app.rabbitmq.routing-key.post-unliked}")
    private String postUnlikedRoutingKey;

    @Value("${app.rabbitmq.routing-key.comment-created}")
    private String commentCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.comment-liked}")
    private String commentLikedRoutingKey;

    @Value("${app.rabbitmq.routing-key.comment-unliked}")
    private String commentUnlikedRoutingKey;

    public void publishPostLiked(PostLikedEvent event) {
        send(postLikedRoutingKey, event, () -> postLikeHandler.handlePostLiked(event));
    }

    public void publishPostUnliked(PostUnlikedEvent event) {
        send(postUnlikedRoutingKey, event, () -> postLikeHandler.handlePostUnliked(event));
    }

    public void publishCommentCreated(CommentCreatedEvent event) {
        send(commentCreatedRoutingKey, event, () -> commentHandler.handleCommentCreated(event));
    }

    public void publishCommentLiked(CommentLikedEvent event) {
        send(commentLikedRoutingKey, event, () -> commentHandler.handleCommentLiked(event));
    }

    public void publishCommentUnliked(CommentUnlikedEvent event) {
        send(commentUnlikedRoutingKey, event, () -> commentHandler.handleCommentUnliked(event));
    }

    private void send(String routingKey, Object event, Runnable applyDirectly) {
        try {
            rabbitTemplate.convertAndSend(socialExchange, routingKey, event);
        } catch (RuntimeException queueFailure) {
            log.warn("Could not queue {} ({}); applying the counter change directly",
                    event.getClass().getSimpleName(), queueFailure.getMessage());
            try {
                applyDirectly.run();
            } catch (RuntimeException directFailure) {
                // The write itself succeeded. A counter that is one off is better than a failed request.
                log.error("Counter for {} was not updated: {}", event.getClass().getSimpleName(), directFailure.getMessage(), directFailure);
            }
        }
    }
}
