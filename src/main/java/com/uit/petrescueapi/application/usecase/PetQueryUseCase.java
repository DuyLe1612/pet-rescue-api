package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.PetQueryDataPort;
import com.uit.petrescueapi.application.port.query.PetQueryPort;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) use-case for Pet operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetQueryUseCase implements PetQueryPort {

    private final PetQueryDataPort queryDataPort;

    @Override
    public PetResponseDto findById(UUID id) {
        log.debug("Query: find pet by id {}", id);
        return queryDataPort.findById(id);
    }

    @Override
    public Page<PetSummaryResponseDto> findAllWithFilters(
            String species,
            String breed,
            String gender,
            String searchName,
            List<PetStatus> statuses,
            UUID ownerUserId,
            UUID ownerOrganizationId,
            Pageable pageable
    ) {
        log.debug("Query: find all pets with filters (species={}, breed={}, gender={}, searchName={}, statuses={}, ownerUserId={}, ownerOrganizationId={})",
                species, breed, gender, searchName, statuses, ownerUserId, ownerOrganizationId);
        return queryDataPort.findAllWithFilters(species, breed, gender, searchName, statuses, ownerUserId, ownerOrganizationId, pageable);
    }

    @Override
    public Page<PetSummaryResponseDto> findAvailableWithFilters(
            String species,
            String breed,
            String gender,
            UUID ownerOrganizationId,
            Pageable pageable
    ) {
        log.debug("Query: find available pets with filters (species={}, breed={}, gender={}, status={}, ownerOrganizationId={})",
                species, breed, gender, PetStatus.AVAILABLE, ownerOrganizationId);
        return queryDataPort.findAvailableWithFilters(species, breed, gender, ownerOrganizationId, pageable);
    }
}
