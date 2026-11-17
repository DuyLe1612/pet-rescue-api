package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.PetTraits;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PetTraitsRepository {

    Optional<PetTraits> findByPetId(UUID petId);

    /** Pets without a record are simply absent from the map. */
    Map<UUID, PetTraits> findByPetIds(Collection<UUID> petIds);

    PetTraits save(PetTraits traits);

    /** English canonical species name of a pet that exists and is not deleted. */
    Optional<String> findSpeciesName(UUID petId);
}
