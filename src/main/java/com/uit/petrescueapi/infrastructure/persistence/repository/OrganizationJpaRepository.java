package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.application.dto.admin.OrganizationStatsDto;
import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

    /**
     * Batch-fetch organizations by IDs in a single query.
     * Optimizes N+1 query problem when fetching multiple pets with organizations.
     */
    @Query("SELECT o FROM OrganizationJpaEntity o WHERE o.organizationId IN :ids")
    List<OrganizationJpaEntity> findAllByOrganizationIdIn(@Param("ids") Set<UUID> ids);

    @Query("""
SELECT
    COALESCE(COUNT(o),0) as total
FROM OrganizationJpaEntity o
""")
    OrganizationStatsProjection getStats();
}
