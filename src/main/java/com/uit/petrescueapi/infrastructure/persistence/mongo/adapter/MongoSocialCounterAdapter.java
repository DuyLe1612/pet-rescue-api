package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.CommentDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Single-document {@code $inc} counters. A decrement only applies while the counter is large
 * enough, so counters never go negative. No triggers exist on this side, so nothing double counts.
 */
@Component
@RequiredArgsConstructor
public class MongoSocialCounterAdapter implements SocialCounterPort {

    private final MongoTemplate mongo;

    @Override
    public void addPostLikes(UUID postId, int delta) {
        add(PostDocument.class, postId, "stats.likes", delta);
    }

    @Override
    public void addPostComments(UUID postId, int delta) {
        add(PostDocument.class, postId, "stats.comments", delta);
    }

    @Override
    public void addCommentLikes(UUID commentId, int delta) {
        add(CommentDocument.class, commentId, "stats.likes", delta);
    }

    @Override
    public void addCommentReplies(UUID commentId, int delta) {
        add(CommentDocument.class, commentId, "stats.replies", delta);
    }

    private void add(Class<?> type, UUID id, String field, int delta) {
        if (delta == 0) {
            return;
        }
        Criteria criteria = where("_id").is(id);
        if (delta < 0) {
            criteria = criteria.and(field).gte(-delta);
        }
        mongo.updateFirst(query(criteria), new Update().inc(field, delta), type);
    }
}
