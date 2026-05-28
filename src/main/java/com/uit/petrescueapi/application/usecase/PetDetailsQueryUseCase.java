package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.media.PetMediaResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetMedicalRecordResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnershipHistoryDisplayDto;
import com.uit.petrescueapi.application.port.out.PetDetailsQueryDataPort;
import com.uit.petrescueapi.application.port.query.PetDetailsQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Query (read) use-case for Pet sub-resource operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link PetDetailsQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetDetailsQueryUseCase implements PetDetailsQueryPort {

    private final PetDetailsQueryDataPort queryDataPort;

    @Override
    public Page<PetMedicalRecordResponseDto> findMedicalRecords(UUID petId, Pageable pageable) {
        log.debug("Query: find medical records for pet {}", petId);
        return queryDataPort.findMedicalRecords(petId, pageable);
    }

    @Override
    public Page<PetOwnershipHistoryDisplayDto> findOwnerships(UUID petId, Pageable pageable) {
        log.debug("Query: find ownerships for pet {}", petId);
        return queryDataPort.findOwnerships(petId, pageable);
    }

    @Override
    public Page<PetMediaResponseDto> findDiaryMedia(UUID petId, Pageable pageable) {
        log.debug("Query: find diary media for pet {}", petId);
        return queryDataPort.findDiaryMedia(petId, pageable);
    }
}
