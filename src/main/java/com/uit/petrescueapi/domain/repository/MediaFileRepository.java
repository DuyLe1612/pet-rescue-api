package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the MediaFile entity.
 */
public interface MediaFileRepository {

    MediaFile save(MediaFile mediaFile);

    Optional<MediaFile> findById(UUID mediaId);

    Optional<MediaFile> findByPublicId(String publicId);

    Page<MediaFile> findByUploaderId(UUID uploaderId, Pageable pageable);

    void delete(UUID mediaId);
}
