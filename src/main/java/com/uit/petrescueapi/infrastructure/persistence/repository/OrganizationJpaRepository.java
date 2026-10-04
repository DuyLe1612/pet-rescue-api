package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.application.dto.admin.OrganizationStatsDto;
import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

    @Query("""
SELECT
    COALESCE(COUNT(o),0) as total
FROM OrganizationJpaEntity o
""")
    OrganizationStatsProjection getStats();
}
