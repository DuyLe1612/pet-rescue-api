package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.repository.PostRepository;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.infrastructure.persistence.entity.PostJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.MediaFileJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PostEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PostJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.MediaFileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {

    private final PostJpaRepository jpa;
    private final PostEntityMapper mapper;
    private final MediaFileJpaRepository mediaFileJpaRepository;
    private final CloudStoragePort cloudStoragePort;

    @Override
    public Post save(Post post) {
        PostJpaEntity entity = mapper.toEntity(post);

        // If client provided mediaIds, resolve the JPA media entities and attach them.
        // Temp media are promoted to permanent storage in the post folder before linking.
        if (post.getMediaIds() != null && !post.getMediaIds().isEmpty()) {
            List<MediaFileJpaEntity> mediaEntities = new ArrayList<>();
            for (UUID mediaId : post.getMediaIds()) {
                MediaFileJpaEntity mediaEntity = mediaFileJpaRepository.findByMediaIdAndDeletedFalse(mediaId)
                        .orElseThrow(() -> new IllegalArgumentException("Media file not found: " + mediaId));

                if ("TEMP".equals(mediaEntity.getStatus())) {
                    String targetFolder = "posts/" + post.getPostId();
                    String newPublicId = cloudStoragePort.moveToPermament(mediaEntity.getPublicId(), targetFolder);
                    mediaEntity.setPublicId(newPublicId);
                    mediaEntity.setStatus("PERMANENT");
                    mediaEntity.setFolder(targetFolder);
                    mediaEntity.setUpdatedAt(LocalDateTime.now());
                    mediaEntity = mediaFileJpaRepository.save(mediaEntity);
                }

                mediaEntities.add(mediaEntity);
            }
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
