package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetSpeciesJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PetSpeciesJpaRepository extends JpaRepository<PetSpeciesJpaEntity, UUID> {

    @Query("""
        SELECT s FROM PetSpeciesJpaEntity s
        WHERE s.deleted = false
          AND (:search IS NULL OR :search = ''
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.nameVi) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<PetSpeciesJpaEntity> search(@Param("search") String search, Pageable pageable);

    List<PetSpeciesJpaEntity> findByDeletedFalse(Sort sort);
}
