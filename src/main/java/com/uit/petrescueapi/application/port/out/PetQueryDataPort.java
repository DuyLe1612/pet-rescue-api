package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.pet.PetAdminResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Output port for Pet read operations (CQRS query side).
 */
public interface PetQueryDataPort {

    PetResponseDto findById(UUID id);

    PetAdminResponseDto findByIdForAdmin(UUID id);

    Page<PetSummaryResponseDto> findAllWithFilters(
            String species,
            String breed,
            String gender,
            String searchName,
            List<PetStatus> statuses,
            UUID ownerUserId,
            UUID ownerOrganizationId,
            Pageable pageable
    );

    Page<PetSummaryResponseDto> findAvailableWithFilters(
            String species,
            String breed,
            String gender,
            UUID ownerOrganizationId,
            Pageable pageable
    );
}
