package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) port for Pet operations.
 */
public interface PetQueryPort {

    PetResponseDto findById(UUID id);

    /**
     * The pet as {@code viewerId} (null for a guest) may see it: a private pet is only found by the
     * people who manage it and by admins, and is reported as not found to everyone else.
     */
    PetResponseDto findByIdForViewer(UUID id, UUID viewerId);

    Page<PetSummaryResponseDto> findOwnedBy(UUID ownerId, com.uit.petrescueapi.domain.valueobject.AcquisitionType type, Pageable pageable);

    Page<PetSummaryResponseDto> findAllWithFilters(
            String species,
            String breed,
            String gender,
            String searchName,
            List<PetStatus> statuses,
            UUID ownerUserId,
            UUID ownerOrganizationId,
            String province,
            /** Comma-separated health statuses; null means any. */
            String healthStatus,
            Pageable pageable
    );

    Page<PetSummaryResponseDto> findAvailableWithFilters(
            String species,
            String breed,
            String gender,
            UUID ownerOrganizationId,
            String province,
            Pageable pageable
    );
}
