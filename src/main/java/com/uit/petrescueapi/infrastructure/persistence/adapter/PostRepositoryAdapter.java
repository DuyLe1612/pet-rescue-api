package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.repository.PostRepository;
import com.uit.petrescueapi.infrastructure.persistence.entity.PostJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PostEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.MediaFileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {

    private final PostJpaRepository jpa;
    private final PostEntityMapper mapper;
    private final MediaFileJpaRepository mediaFileJpaRepository;

    @Override
    public Post save(Post post) {
        PostJpaEntity entity = mapper.toEntity(post);

        // If client provided mediaIds, resolve the JPA media entities and attach them
        if (post.getMediaIds() != null && !post.getMediaIds().isEmpty()) {
            var mediaEntities = mediaFileJpaRepository.findAllById(post.getMediaIds());
            entity.setMediaFiles(mediaEntities);
        }

        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Post> findById(UUID postId) {
        return jpa.findById(postId)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Page<Post> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void delete(UUID postId) {
        jpa.findById(postId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(LocalDateTime.now());
            jpa.save(entity);
        });
    }
}
