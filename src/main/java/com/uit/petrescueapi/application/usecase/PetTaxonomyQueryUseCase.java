package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.pet.PetBreedResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSpeciesResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetTaxonomyResponseDto;
import com.uit.petrescueapi.application.port.out.PetTaxonomyQueryDataPort;
import com.uit.petrescueapi.application.port.query.PetTaxonomyQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetTaxonomyQueryUseCase implements PetTaxonomyQueryPort {

    private final PetTaxonomyQueryDataPort queryDataPort;

    @Override
    public Page<PetSpeciesResponseDto> findSpecies(String search, Pageable pageable) {
        log.debug("Query: find pet species (search={})", search);
        return queryDataPort.findSpecies(search, pageable);
    }

    @Override
    public Page<PetBreedResponseDto> findBreeds(UUID speciesId, String search, Pageable pageable) {
        log.debug("Query: find breeds of species {} (search={})", speciesId, search);
        return queryDataPort.findBreeds(speciesId, search, pageable);
    }

    @Override
    public List<PetTaxonomyResponseDto> findTaxonomy() {
        log.debug("Query: find full pet taxonomy");
        return queryDataPort.findTaxonomy();
    }
}
