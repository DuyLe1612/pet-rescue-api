package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.domain.entity.PostLike;
import com.uit.petrescueapi.domain.repository.PostLikeRepository;
import com.uit.petrescueapi.infrastructure.config.PostOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LikeDocuments.PostLikeDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.LikeDocuments.PostLikeKey;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Component
@PostOnMongo
@RequiredArgsConstructor
public class MongoPostLikeRepositoryAdapter implements PostLikeRepository {

    private final MongoTemplate mongo;

    @Override
    public PostLike save(PostLike like) {
        try {
            mongo.insert(new PostLikeDocument(new PostLikeKey(like.getPostId(), like.getUserId()), like.getCreatedAt()));
        } catch (DuplicateKeyException alreadyLiked) {
            // compound _id makes a second like a no-op, same as the old composite PK
        }
        return like;
    }

    @Override
    public boolean exists(UUID postId, UUID userId) {
        return mongo.exists(query(where("_id").is(new PostLikeKey(postId, userId))), PostLikeDocument.class);
    }

    @Override
    public void delete(UUID postId, UUID userId) {
        mongo.remove(query(where("_id").is(new PostLikeKey(postId, userId))), PostLikeDocument.class);
    }
}
