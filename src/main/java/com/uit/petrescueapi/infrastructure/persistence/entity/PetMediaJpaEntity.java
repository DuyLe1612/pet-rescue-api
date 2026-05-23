package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code pet_media} table.
 * References media_files via media_file_id - URL is built from public_id (single source of truth).
 * Lightweight media reference — does not extend BaseJpaEntity.
 */
@Entity
@Table(name = "pet_media", indexes = {
        @Index(name = "idx_pet_media_pet", columnList = "pet_id"),
        @Index(name = "idx_pet_media_media_file", columnList = "media_file_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetMediaJpaEntity {

    @Id
    @Column(name = "media_id", updatable = false, nullable = false)
    private UUID mediaId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "media_file_id")
    private UUID mediaFileId;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primaryMedia = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Read-only association used by JOIN queries only.
     * Persistence is handled by the {@code petId} field above.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private PetJpaEntity pet;

    /**
     * Read-only association to media_files.
     * Persistence is handled by the {@code mediaFileId} field above.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", insertable = false, updatable = false)
    private MediaFileJpaEntity mediaFile;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
