package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetOwnershipJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.PetOwnershipId;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetOwnershipHistoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetOwnershipJpaRepository extends JpaRepository<PetOwnershipJpaEntity, PetOwnershipId> {

    Optional<PetOwnershipJpaEntity> findByPetIdAndToTimeIsNull(UUID petId);

    List<PetOwnershipJpaEntity> findAllByPetIdOrderByFromTimeDesc(UUID petId);

    @Query(value = """
            SELECT po.owner_type AS ownerType,
                   COALESCE(NULLIF(u.full_name, ''), u.username, u.email, own_org.name, 'Unknown owner') AS ownerName,
                   po.from_time AS historyTimestamp
            FROM pet_ownerships po
            LEFT JOIN users u ON po.owner_type = 'USER' AND po.owner_id = u.user_id
            LEFT JOIN organizations own_org ON po.owner_type = 'ORGANIZATION' AND po.owner_id = own_org.organization_id
            WHERE po.pet_id = :petId
            ORDER BY po.from_time DESC
            """,
            countQuery = "SELECT COUNT(*) FROM pet_ownerships po WHERE po.pet_id = :petId",
            nativeQuery = true)
    org.springframework.data.domain.Page<PetOwnershipHistoryProjection> findHistoryByPetId(@Param("petId") UUID petId,
                                                                                           org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("UPDATE PetOwnershipJpaEntity o SET o.toTime = :endTime WHERE o.petId = :petId AND o.toTime IS NULL")
    void endCurrentOwnership(@Param("petId") UUID petId, @Param("endTime") LocalDateTime endTime);
}
