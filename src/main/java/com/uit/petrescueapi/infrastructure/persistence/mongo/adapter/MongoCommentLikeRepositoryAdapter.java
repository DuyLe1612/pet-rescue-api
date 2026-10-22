package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.domain.entity.CommentLike;
import com.uit.petrescueapi.domain.repository.CommentLikeRepository;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LikeDocuments.CommentLikeDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LikeDocuments.CommentLikeKey;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Component
@RequiredArgsConstructor
public class MongoCommentLikeRepositoryAdapter implements CommentLikeRepository {

    private final MongoTemplate mongo;

    @Override
    public CommentLike save(CommentLike like) {
        try {
            mongo.insert(new CommentLikeDocument(new CommentLikeKey(like.getCommentId(), like.getUserId()), like.getCreatedAt()));
        } catch (DuplicateKeyException alreadyLiked) {
            // compound _id makes a second like a no-op
        }
        return like;
    }

    @Override
    public boolean exists(UUID commentId, UUID userId) {
        return mongo.exists(query(where("_id").is(new CommentLikeKey(commentId, userId))), CommentLikeDocument.class);
    }

    @Override
    public void delete(UUID commentId, UUID userId) {
        mongo.remove(query(where("_id").is(new CommentLikeKey(commentId, userId))), CommentLikeDocument.class);
    }
}
