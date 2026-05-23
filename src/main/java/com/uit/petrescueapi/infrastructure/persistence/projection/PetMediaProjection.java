package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface projection for pet media with media file details.
 * Used for optimized JOIN queries that fetch media + public_id in one query.
 */
public interface PetMediaProjection {

    UUID getMediaId();

    UUID getPetId();

    UUID getMediaFileId();

    String getType();

    boolean isPrimaryMedia();

    LocalDateTime getCreatedAt();

    // From joined media_files table
    String getPublicId();
}
