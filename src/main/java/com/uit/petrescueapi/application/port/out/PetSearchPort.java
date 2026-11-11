package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Output port for pluggable pet search providers (e.g., full-text engines).
 * Implementations may call external search services or fallback to DB queries.
 */
public interface PetSearchPort {
    Page<PetSummaryResponseDto> searchPets(
            String searchName,
            String species,
            String breed,
            String gender,
            List<String> statuses,
            UUID ownerUserId,
            UUID ownerOrganizationId,
            String province,
            Pageable pageable
    );
}
