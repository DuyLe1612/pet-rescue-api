package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.MediaFile;
import com.uit.petrescueapi.domain.repository.MediaFileRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.MediaFileEntityMapper;
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
public class MediaFileRepositoryAdapter implements MediaFileRepository {

    private final MediaFileJpaRepository jpa;
    private final MediaFileEntityMapper mapper;

    @Override
    public MediaFile save(MediaFile mediaFile) {
        return mapper.toDomain(jpa.save(mapper.toEntity(mediaFile)));
    }

    @Override
    public Optional<MediaFile> findById(UUID mediaId) {
        return jpa.findById(mediaId)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<MediaFile> findByPublicId(String publicId) {
        return jpa.findByPublicIdAndDeletedFalse(publicId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MediaFile> findByUploaderId(UUID uploaderId, Pageable pageable) {
        // Can be replaced with a dedicated JPA query later
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(UUID mediaId) {
        jpa.findById(mediaId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(LocalDateTime.now());
            jpa.save(entity);
        });
    }
}
