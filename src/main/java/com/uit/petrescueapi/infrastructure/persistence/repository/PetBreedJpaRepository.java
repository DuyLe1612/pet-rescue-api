package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetBreedJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetBreedJpaRepository extends JpaRepository<PetBreedJpaEntity, UUID> {

    @Query("""
        SELECT b FROM PetBreedJpaEntity b
        WHERE b.deleted = false
          AND b.speciesId = :speciesId
          AND (:search IS NULL OR :search = ''
               OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.nameVi) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<PetBreedJpaEntity> search(@Param("speciesId") UUID speciesId, @Param("search") String search, Pageable pageable);

    List<PetBreedJpaEntity> findByDeletedFalse(Sort sort);

    Optional<PetBreedJpaEntity> findByBreedIdAndSpeciesIdAndDeletedFalse(UUID breedId, UUID speciesId);
}
