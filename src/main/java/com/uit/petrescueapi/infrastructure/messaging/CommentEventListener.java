package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.domain.event.CommentCreatedEvent;
import com.uit.petrescueapi.domain.event.CommentLikedEvent;
import com.uit.petrescueapi.domain.event.CommentUnlikedEvent;
import com.uit.petrescueapi.infrastructure.redis.RedisCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentEventListener {

    private final RedisCounterService redisCounterService;
    private final SocialCounterPort counters;

    @RabbitListener(queues = "${app.rabbitmq.queue.comment-created}")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisCounterService.incrementPostCommentCount(event.getPostId());
        counters.addPostComments(event.getPostId(), 1);

        if (event.getParentCommentId() != null) {
            redisCounterService.incrementCommentReplyCount(event.getParentCommentId());
            counters.addCommentReplies(event.getParentCommentId(), 1);
        }

        log.debug("Handled CommentCreatedEvent: commentId={}, postId={}, parentCommentId={}",
                event.getCommentId(), event.getPostId(), event.getParentCommentId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.comment-liked}")
    public void handleCommentLiked(CommentLikedEvent event) {
        boolean added = redisCounterService.incrementCommentLikes(event.getCommentId(), event.getUserId());
        if (added) {
            counters.addCommentLikes(event.getCommentId(), 1);
        }

        log.debug("Handled CommentLikedEvent: commentId={}, userId={}, redisAdded={}",
                event.getCommentId(), event.getUserId(), added);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.comment-unliked}")
    public void handleCommentUnliked(CommentUnlikedEvent event) {
        boolean removed = redisCounterService.decrementCommentLikes(event.getCommentId(), event.getUserId());
        if (removed) {
            counters.addCommentLikes(event.getCommentId(), -1);
        }

        log.debug("Handled CommentUnlikedEvent: commentId={}, userId={}, redisRemoved={}",
                event.getCommentId(), event.getUserId(), removed);
    }
}
