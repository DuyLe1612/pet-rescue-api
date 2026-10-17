package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.pet.PetBreedResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSpeciesResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetTaxonomyResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Output port for pet species/breed read operations (CQRS query side).
 */
public interface PetTaxonomyQueryDataPort {

    Page<PetSpeciesResponseDto> findSpecies(String search, Pageable pageable);

    Page<PetBreedResponseDto> findBreeds(UUID speciesId, String search, Pageable pageable);

    List<PetTaxonomyResponseDto> findTaxonomy();
}
