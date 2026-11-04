package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.domain.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MediaCommandPort {
    MediaFile register(UUID uploaderId, String publicId, String resourceType, String format, Integer width, Integer height, Integer bytes, String folder);
    
    /**
     * Upload a file to temporary storage.
     * File will be moved to permanent storage when confirmed.
     */
    MediaFile uploadTemp(MultipartFile file, UUID uploaderId, String subFolder);
    
    /**
     * Confirm a temp upload and move to permanent storage.
     */
    MediaFile confirmUpload(UUID mediaId, String targetFolder);
    
    void delete(UUID mediaId);

    /** Confirm requested over the API: only the uploader or an admin. */
    MediaFile confirmUpload(UUID mediaId, String targetFolder, UUID requesterId);

    /** Delete requested over the API: only the uploader or an admin. */
    void delete(UUID mediaId, UUID requesterId);
}
