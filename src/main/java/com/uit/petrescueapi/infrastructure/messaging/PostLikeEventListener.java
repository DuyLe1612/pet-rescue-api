package com.uit.petrescueapi.infrastructure.messaging;

import com.uit.petrescueapi.domain.event.PostLikedEvent;
import com.uit.petrescueapi.domain.event.PostUnlikedEvent;
import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.infrastructure.redis.RedisCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostLikeEventListener {

    private final RedisCounterService redisCounterService;
    private final SocialCounterPort counters;

    @RabbitListener(queues = "${app.rabbitmq.queue.post-liked}")
    public void handlePostLiked(PostLikedEvent event) {
        boolean added = redisCounterService.incrementPostLikes(event.getPostId(), event.getUserId());
        if (added) {
            counters.addPostLikes(event.getPostId(), 1);
        }
        log.debug("Handled PostLikedEvent: postId={}, userId={}, redisAdded={}",
                event.getPostId(), event.getUserId(), added);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.post-unliked}")
    public void handlePostUnliked(PostUnlikedEvent event) {
        boolean removed = redisCounterService.decrementPostLikes(event.getPostId(), event.getUserId());
        if (removed) {
            counters.addPostLikes(event.getPostId(), -1);
        }
        log.debug("Handled PostUnlikedEvent: postId={}, userId={}, redisRemoved={}",
                event.getPostId(), event.getUserId(), removed);
    }
}
