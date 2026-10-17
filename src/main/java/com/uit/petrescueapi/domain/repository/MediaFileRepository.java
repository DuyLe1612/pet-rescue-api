package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.MediaFile;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the MediaFile entity.
 */
public interface MediaFileRepository {

    MediaFile save(MediaFile mediaFile);

    Optional<MediaFile> findById(UUID mediaId);

    Optional<MediaFile> findByPublicId(String publicId);

    void delete(UUID mediaId);
}
