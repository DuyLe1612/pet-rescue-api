package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.PetJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PetJpaEntity}.
 * Used by the <b>command-side</b> adapter ({@link com.uit.petrescueapi.infrastructure.persistence.adapter.PetRepositoryAdapter}).
 *
 * <p>For read/query-side operations, see {@link PetQueryJpaRepository}.</p>
 */
@Repository
public interface PetJpaRepository extends JpaRepository<PetJpaEntity, UUID> {

    @Query("SELECT p FROM PetJpaEntity p WHERE p.deleted = false AND p.id = :id")
    Optional<PetJpaEntity> findByIdAndNotDeleted(@Param("id") UUID id);

    @Query("SELECT p FROM PetJpaEntity p WHERE p.deleted = false ORDER BY p.createdAt DESC")
    List<PetJpaEntity> findAllNotDeleted();

    @Query("SELECT p FROM PetJpaEntity p WHERE p.deleted = false")
    Page<PetJpaEntity> findAllNotDeleted(Pageable pageable);

    @Query("SELECT p FROM PetJpaEntity p WHERE p.deleted = false AND p.status = :status")
    List<PetJpaEntity> findByStatus(@Param("status") PetStatus status);

    @Query("SELECT p FROM PetJpaEntity p WHERE p.deleted = false AND p.status = :status")
    Page<PetJpaEntity> findByStatus(@Param("status") PetStatus status, Pageable pageable);

    @Query("""

            SELECT
    COALESCE(COUNT(p), 0) as total,
    COALESCE(SUM(CASE WHEN p.status = 'UNOWNED' THEN 1 ELSE 0 END), 0) as available,
    COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN 1 ELSE 0 END), 0) as pending,
    COALESCE(SUM(CASE WHEN p.status = 'ADOPTED' THEN 1 ELSE 0 END), 0) as adopted
FROM PetJpaEntity p
WHERE p.deleted = false
""")
    PetStatsProjection getStats();

}
