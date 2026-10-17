package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.infrastructure.config.PostOnPostgres;
import com.uit.petrescueapi.domain.entity.CommentLike;
import com.uit.petrescueapi.domain.repository.CommentLikeRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.CommentLikeEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.CommentLikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@PostOnPostgres
public class CommentLikeRepositoryAdapter implements CommentLikeRepository {

    private final CommentLikeJpaRepository jpa;
    private final CommentLikeEntityMapper mapper;

    @Override
    public CommentLike save(CommentLike commentLike) {
        return mapper.toDomain(jpa.save(mapper.toEntity(commentLike)));
    }

    @Override
    public boolean exists(UUID commentId, UUID userId) {
        return jpa.existsByCommentIdAndUserId(commentId, userId);
    }

    @Override
    public void delete(UUID commentId, UUID userId) {
        jpa.deleteByCommentIdAndUserId(commentId, userId);
    }
}
