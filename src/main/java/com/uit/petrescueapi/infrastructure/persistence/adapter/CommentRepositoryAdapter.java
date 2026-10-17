package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.infrastructure.config.PostOnPostgres;
import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.repository.CommentRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.CommentEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.CommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@PostOnPostgres
public class CommentRepositoryAdapter implements CommentRepository {

    private final CommentJpaRepository jpa;
    private final CommentEntityMapper mapper;

    @Override
    public Comment save(Comment comment) {
        return mapper.toDomain(jpa.save(mapper.toEntity(comment)));
    }

    @Override
    public Optional<Comment> findById(UUID commentId) {
        return jpa.findById(commentId)
                .filter(entity -> !entity.isDeleted())
                .map(mapper::toDomain);
    }
}
