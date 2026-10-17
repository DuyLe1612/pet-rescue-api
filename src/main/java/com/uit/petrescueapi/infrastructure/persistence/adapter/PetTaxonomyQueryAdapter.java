package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.pet.PetBreedResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSpeciesResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetTaxonomyResponseDto;
import com.uit.petrescueapi.application.port.out.PetTaxonomyQueryDataPort;
import com.uit.petrescueapi.infrastructure.persistence.entity.PetBreedJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.PetSpeciesJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetBreedJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetSpeciesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query-side adapter (CQRS read path) for pet species and breeds.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetTaxonomyQueryAdapter implements PetTaxonomyQueryDataPort {

    private static final Sort BY_NAME = Sort.by("name");

    private final PetSpeciesJpaRepository speciesRepository;
    private final PetBreedJpaRepository breedRepository;

    @Override
    public Page<PetSpeciesResponseDto> findSpecies(String search, Pageable pageable) {
        return speciesRepository.search(search, pageable).map(this::toSpeciesDto);
    }

    @Override
    public Page<PetBreedResponseDto> findBreeds(UUID speciesId, String search, Pageable pageable) {
        return breedRepository.search(speciesId, search, pageable).map(this::toBreedDto);
    }

    @Override
    public List<PetTaxonomyResponseDto> findTaxonomy() {
        Map<UUID, List<PetBreedResponseDto>> breedsBySpecies = breedRepository.findByDeletedFalse(BY_NAME).stream()
                .map(this::toBreedDto)
                .collect(Collectors.groupingBy(PetBreedResponseDto::getSpeciesId));

        return speciesRepository.findByDeletedFalse(BY_NAME).stream()
                .map(species -> PetTaxonomyResponseDto.builder()
                        .speciesId(species.getSpeciesId())
                        .name(species.getName())
                        .nameVi(species.getNameVi())
                        .breeds(breedsBySpecies.getOrDefault(species.getSpeciesId(), List.of()))
                        .build())
                .toList();
    }

    private PetSpeciesResponseDto toSpeciesDto(PetSpeciesJpaEntity entity) {
        return PetSpeciesResponseDto.builder()
                .speciesId(entity.getSpeciesId())
                .name(entity.getName())
                .nameVi(entity.getNameVi())
                .build();
    }

    private PetBreedResponseDto toBreedDto(PetBreedJpaEntity entity) {
        return PetBreedResponseDto.builder()
                .breedId(entity.getBreedId())
                .speciesId(entity.getSpeciesId())
                .name(entity.getName())
                .nameVi(entity.getNameVi())
                .build();
    }
}
