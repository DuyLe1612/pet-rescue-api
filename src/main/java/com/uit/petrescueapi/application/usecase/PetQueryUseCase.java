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
    public Page<PetSummaryResponseDto> findAll(Pageable pageable) {
        log.debug("Query: find all pets (paginated)");
        return queryDataPort.findAllWithFilters(null, null, null, null, null, null, null, pageable);
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
    public Page<PetSummaryResponseDto> findAvailable(Pageable pageable) {
        log.debug("Query: find available pets (paginated)");
        return queryDataPort.findAvailableWithFilters(null, null, null, null, pageable);
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

    @Override
    public Page<PetSummaryResponseDto> findByOrganizationId(
            UUID organizationId,
            String species,
            String breed,
            String gender,
            Pageable pageable
    ) {
        log.debug("Query: find pets by organization id {} with filters (species={}, breed={}, gender={})",
                organizationId, species, breed, gender);
        return queryDataPort.findSummariesByOrganizationId(organizationId, species, breed, gender, pageable);
    }

    @Override
    public Page<PetSummaryResponseDto> findByUserId(
            UUID userId,
            String species,
            String breed,
            String gender,
            Pageable pageable
    ) {
        log.debug("Query: find pets by user id {} with filters (species={}, breed={}, gender={})",
                userId, species, breed, gender);
        return queryDataPort.findSummariesByUserId(userId, species, breed, gender, pageable);
    }
}
