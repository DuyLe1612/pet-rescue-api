package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.media.PetMediaResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetMedicalRecordResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnershipHistoryDisplayDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Query (read) port for Pet sub-resource operations.
 * Handles medical records, ownerships and diary queries.
 */
public interface PetDetailsQueryPort {

    Page<PetMedicalRecordResponseDto> findMedicalRecords(UUID petId, Pageable pageable);

    Page<PetOwnershipHistoryDisplayDto> findOwnerships(UUID petId, Pageable pageable);

    Page<PetMediaResponseDto> findDiaryMedia(UUID petId, Pageable pageable);
}
