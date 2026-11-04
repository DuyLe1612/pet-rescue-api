package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.domain.entity.MediaFile;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service encapsulating MediaFile business rules.
 *
 * Rules:
 *  - New media files get a generated id and createdAt timestamp.
 *  - Temp files can be confirmed (moved to permanent storage).
 *  - Deletion is soft-delete (sets deleted = true).
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MediaFileDomainService {

    private final MediaFileRepository mediaFileRepository;
    private final CloudStoragePort cloudStoragePort;

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public MediaFile findById(UUID mediaId) {
        return mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaFile", "mediaId", mediaId));
    }

    // ── Commands ────────────────────────────────────

    /**
     * Register a new media file.
     * Sets the id and createdAt timestamp.
     */
    public MediaFile register(MediaFile mediaFile) {
        log.info("Registering media file for uploader {}", mediaFile.getUploaderId());
        mediaFile.setMediaId(UUID.randomUUID());
        mediaFile.setCreatedAt(LocalDateTime.now());
        if (mediaFile.getStatus() == null) {
            mediaFile.setStatus("PERMANENT");
        }
        return mediaFileRepository.save(mediaFile);
    }

    /** Records a file already moved to permanent storage. The move itself must not run in a transaction. */
    public MediaFile markPermanent(UUID mediaId, String newPublicId, String targetFolder) {
        MediaFile mediaFile = findById(mediaId);
        mediaFile.setPublicId(newPublicId);
mediaFile.setStatus("PERMANENT");
        mediaFile.setFolder(targetFolder);
        mediaFile.setUpdatedAt(LocalDateTime.now());
        
        return mediaFileRepository.save(mediaFile);
    }

    /**
     * Soft delete a media file.
     * Also deletes the file from cloud storage.
     */
    public void delete(UUID mediaId) {
        MediaFile mediaFile = findById(mediaId);
        
        // Delete from cloud storage
        try {
            cloudStoragePort.delete(mediaFile.getPublicId());
        } catch (Exception e) {
            log.warn("Failed to delete media from cloud storage: {}", e.getMessage());
        }
        
        mediaFile.setDeleted(true);
        mediaFile.setDeletedAt(LocalDateTime.now());
        mediaFileRepository.save(mediaFile);
        log.info("Soft-deleted media file {}", mediaId);
    }
}
