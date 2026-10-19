package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.infrastructure.config.PostOnPostgres;
import com.uit.petrescueapi.domain.entity.PostLike;
import com.uit.petrescueapi.domain.repository.PostLikeRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PostLikeEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostLikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@PostOnPostgres
public class PostLikeRepositoryAdapter implements PostLikeRepository {

    private final PostLikeJpaRepository jpa;
    private final PostLikeEntityMapper mapper;

    @Override
    public PostLike save(PostLike postLike) {
        return mapper.toDomain(jpa.save(mapper.toEntity(postLike)));
    }

    @Override
    public boolean exists(UUID postId, UUID userId) {
        return jpa.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    public void delete(UUID postId, UUID userId) {
        jpa.deleteByPostIdAndUserId(postId, userId);
    }
}
