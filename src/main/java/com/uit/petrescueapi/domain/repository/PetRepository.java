package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for Pet aggregate.
 *
 * This is a pure domain interface (a "port").
 * The infrastructure layer provides the concrete implementation.
 */
public interface PetRepository {

    Pet save(Pet pet);

    Optional<Pet> findById(UUID id);

    List<Pet> findAll();

    Page<Pet> findAll(Pageable pageable);

    List<Pet> findByStatus(PetStatus status);

    Page<Pet> findByStatus(PetStatus status, Pageable pageable);
}
