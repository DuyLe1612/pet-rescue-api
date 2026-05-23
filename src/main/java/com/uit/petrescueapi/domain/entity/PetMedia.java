package com.uit.petrescueapi.domain.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PetMedia — represents a media attachment (image/video) associated with a pet.
 * References media_files via mediaFileId - URL is built from public_id (single source of truth).
 * Pure domain entity: no JPA annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetMedia {

    private UUID mediaId;
    private UUID petId;
    private UUID mediaFileId;  // Reference to media_files table
    private String type;
    private boolean primaryMedia;
    private LocalDateTime createdAt;
}
