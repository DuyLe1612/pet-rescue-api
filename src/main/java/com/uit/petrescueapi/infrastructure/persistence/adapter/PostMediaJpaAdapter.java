package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PostMediaPort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.infrastructure.persistence.entity.MediaFileJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.MediaFileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Promotes TEMP uploads to PERMANENT when they are attached to a (MongoDB) post.
 * {@code media_files} stays in PostgreSQL, so this runs in its own JPA transaction.
 */
@Component
@RequiredArgsConstructor
public class PostMediaJpaAdapter implements PostMediaPort {

    private final MediaFileJpaRepository mediaFileJpaRepository;
    private final CloudStoragePort cloudStoragePort;

    @Override
    @Transactional
    public List<AttachedMedia> attach(UUID postId, List<UUID> mediaIds) {
        List<MediaFileJpaEntity> entities = new ArrayList<>();
        for (UUID mediaId : mediaIds) {
            entities.add(mediaFileJpaRepository.findByMediaIdAndDeletedFalse(mediaId)
                    .orElseThrow(() -> new BusinessException("Unknown media: " + mediaId, "POST_MEDIA_NOT_FOUND")));
        }
        String targetFolder = "posts/" + postId;
        List<AttachedMedia> attached = new ArrayList<>();
        for (MediaFileJpaEntity media : entities) {
            if ("TEMP".equals(media.getStatus())) {
                media.setPublicId(cloudStoragePort.moveToPermament(media.getPublicId(), targetFolder, media.getResourceType()));
                media.setStatus("PERMANENT");
                media.setFolder(targetFolder);
                media.setUpdatedAt(LocalDateTime.now());
                media = mediaFileJpaRepository.save(media);
            }
            attached.add(new AttachedMedia(media.getMediaId(), media.getPublicId()));
        }
        return attached;
    }
}
