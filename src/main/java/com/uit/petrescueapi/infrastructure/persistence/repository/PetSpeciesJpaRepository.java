package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetSpeciesJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetSpeciesJpaRepository extends JpaRepository<PetSpeciesJpaEntity, UUID> {
    Page<PetSpeciesJpaEntity> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
    Page<PetSpeciesJpaEntity> findByDeletedFalse(Pageable pageable);
}
