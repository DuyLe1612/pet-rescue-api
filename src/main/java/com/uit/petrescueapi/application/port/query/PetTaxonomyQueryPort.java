package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.pet.PetBreedResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSpeciesResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetTaxonomyResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) port for pet species and breeds.
 */
public interface PetTaxonomyQueryPort {

    Page<PetSpeciesResponseDto> findSpecies(String search, Pageable pageable);

    Page<PetBreedResponseDto> findBreeds(UUID speciesId, String search, Pageable pageable);

    List<PetTaxonomyResponseDto> findTaxonomy();
}
