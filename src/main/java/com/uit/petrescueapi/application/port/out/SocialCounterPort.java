package com.uit.petrescueapi.application.port.out;

import java.util.UUID;

/**
 * Denormalised social counters (likes, comments, replies). Called by the RabbitMQ listeners
 * after Redis has de-duplicated the event. Implementations never let a counter go below zero.
 */
public interface SocialCounterPort {

    void addPostLikes(UUID postId, int delta);

    void addPostComments(UUID postId, int delta);

    void addCommentLikes(UUID commentId, int delta);

    void addCommentReplies(UUID commentId, int delta);
}
