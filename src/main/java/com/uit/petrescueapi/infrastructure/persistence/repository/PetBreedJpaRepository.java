package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetBreedJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PetBreedJpaRepository extends JpaRepository<PetBreedJpaEntity, UUID> {
    Page<PetBreedJpaEntity> findBySpeciesIdAndDeletedFalse(UUID speciesId, Pageable pageable);
    Page<PetBreedJpaEntity> findBySpeciesIdAndNameContainingIgnoreCaseAndDeletedFalse(UUID speciesId, String name, Pageable pageable);
    Optional<PetBreedJpaEntity> findByBreedIdAndSpeciesIdAndDeletedFalse(UUID breedId, UUID speciesId);
}
