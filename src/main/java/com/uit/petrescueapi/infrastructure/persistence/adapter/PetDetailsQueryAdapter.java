package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.media.PetMediaResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetMedicalRecordResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnershipHistoryDisplayDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PetDetailsQueryDataPort;
import com.uit.petrescueapi.infrastructure.persistence.entity.PetMedicalRecordJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetMediaProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetOwnershipHistoryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetOwnershipJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetMediaJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetMedicalRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Query-side adapter (CQRS read path) for Pet sub-resource details.
 *
 * <p>Handles medical records, ownership history, and diary media
 * for a specific pet.</p>
 *
 * <p>{@code @Transactional(readOnly = true)} ensures Hibernate session stays open
 * during mapping operations.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetDetailsQueryAdapter implements PetDetailsQueryDataPort {

    private final PetMedicalRecordJpaRepository petMedicalRecordJpaRepo;
    private final PetOwnershipJpaRepository petOwnershipJpaRepo;
    private final PetMediaJpaRepository petMediaJpaRepo;
    private final CloudStoragePort cloudStoragePort;

    // ── Medical records ─────────────────────────

    @Override
    public Page<PetMedicalRecordResponseDto> findMedicalRecords(UUID petId, Pageable pageable) {
        return petMedicalRecordJpaRepo.findByPetIdAndDeletedFalse(petId, pageable)
                .map(this::toMedicalRecordDto);
    }

    // ── Ownerships ──────────────────────────────

    @Override
    public Page<PetOwnershipHistoryDisplayDto> findOwnerships(UUID petId, Pageable pageable) {
        return petOwnershipJpaRepo.findHistoryByPetId(petId, pageable)
                .map(this::toOwnershipHistoryDto);
    }

    // ── Diary media ─────────────────────────────

    /**
     * Optimized: Uses single JOIN query instead of N+1 queries.
     * Database pagination instead of in-memory pagination.
     *
     * <p>Before: 1 + N queries (fetch all, then fetch media_file for each)
     * <p>After: 1 query (JOIN pet_media with media_files, paginated)
     */
    @Override
    public Page<PetMediaResponseDto> findDiaryMedia(UUID petId, Pageable pageable) {
        return petMediaJpaRepo.findMediaWithFilesByPetId(petId, pageable)
                .map(this::toMediaDto);
    }

    // ── Projection/Entity → DTO mappers ─────────

    private PetMedicalRecordResponseDto toMedicalRecordDto(PetMedicalRecordJpaEntity e) {
        return PetMedicalRecordResponseDto.builder()
                .recordId(e.getRecordId())
                .petId(e.getPetId())
                .description(e.getDescription())
                .vaccine(e.getVaccine())
                .diagnosis(e.getDiagnosis())
                .recordDate(e.getRecordDate())
                .createdBy(e.getCreatedBy())
                .build();
    }

    private PetOwnershipHistoryDisplayDto toOwnershipHistoryDto(PetOwnershipHistoryProjection projection) {
        return PetOwnershipHistoryDisplayDto.builder()
                .ownerType(projection.getOwnerType())
                .ownerName(projection.getOwnerName())
                .timestamp(projection.getHistoryTimestamp())
                .build();
    }

    /**
     * Maps projection (from JOIN query) to DTO.
     * URL is built from publicId already included in projection — no extra query!
     */
    private PetMediaResponseDto toMediaDto(PetMediaProjection p) {
        String url = (p.getPublicId() != null)
                ? cloudStoragePort.buildUrl(p.getPublicId())
                : null;

        return PetMediaResponseDto.builder()
                .mediaId(p.getMediaId())
                .petId(p.getPetId())
                .mediaFileId(p.getMediaFileId())
                .url(url)
                .type(p.getType())
                .primaryMedia(p.isPrimaryMedia())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
