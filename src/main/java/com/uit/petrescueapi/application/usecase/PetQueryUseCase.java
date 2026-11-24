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
    private final com.uit.petrescueapi.domain.service.AccessControlDomainService accessControl;

    @Override
    public PetResponseDto findById(UUID id) {
        log.debug("Query: find pet by id {}", id);
        return queryDataPort.findById(id);
    }

    @Override
    public PetResponseDto findByIdForViewer(UUID id, UUID viewerId) {
        PetResponseDto pet = queryDataPort.findById(id);
        if (pet.getVisibility() == com.uit.petrescueapi.domain.valueobject.PetVisibility.PRIVATE
                && !accessControl.canManagePet(id, viewerId)) {
            throw new com.uit.petrescueapi.domain.exception.ResourceNotFoundException("Pet", "id", id);
        }
        return pet;
    }

    @Override
    public Page<PetSummaryResponseDto> findOwnedBy(UUID ownerId, com.uit.petrescueapi.domain.valueobject.AcquisitionType type, Pageable pageable) {
        return queryDataPort.findOwnedBy(ownerId, type, pageable);
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
            String province,
            String healthStatus,
            Pageable pageable
    ) {
        log.debug("Query: find all pets with filters (species={}, breed={}, gender={}, searchName={}, statuses={}, ownerUserId={}, ownerOrganizationId={})",
                species, breed, gender, searchName, statuses, ownerUserId, ownerOrganizationId);
        return queryDataPort.findAllWithFilters(species, breed, gender, searchName, statuses, ownerUserId, ownerOrganizationId, province, healthStatus, pageable);
    }

    @Override
    public Page<PetSummaryResponseDto> findAvailableWithFilters(
            String species,
            String breed,
            String gender,
            UUID ownerOrganizationId,
            String province,
            Pageable pageable
    ) {
        log.debug("Query: find available pets with filters (species={}, breed={}, gender={}, status={}, ownerOrganizationId={})",
                species, breed, gender, PetStatus.AVAILABLE, ownerOrganizationId);
        return queryDataPort.findAvailableWithFilters(species, breed, gender, ownerOrganizationId, province, pageable);
    }
}
