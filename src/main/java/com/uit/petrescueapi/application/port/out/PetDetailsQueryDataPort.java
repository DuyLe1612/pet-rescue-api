package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.media.PetMediaResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetMedicalRecordResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnershipResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Output port for Pet sub-resource read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * queries and map projections directly to application DTOs.</p>
 */
public interface PetDetailsQueryDataPort {

    Page<PetMedicalRecordResponseDto> findMedicalRecords(UUID petId, Pageable pageable);

    Page<PetOwnershipResponseDto> findOwnerships(UUID petId, Pageable pageable);

    Page<PetMediaResponseDto> findDiaryMedia(UUID petId, Pageable pageable);
}
