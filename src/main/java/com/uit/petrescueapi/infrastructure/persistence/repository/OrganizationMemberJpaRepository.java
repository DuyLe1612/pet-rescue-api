package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationMemberId;
import com.uit.petrescueapi.infrastructure.persistence.entity.OrganizationMemberJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.OrganizationMemberProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberJpaRepository
        extends JpaRepository<OrganizationMemberJpaEntity, OrganizationMemberId> {

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

        Optional<OrganizationMemberJpaEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    /**
     * Find the first (primary) organization for a user.
     * Used to get organizationId for JWT when user has MEMBER role.
     */
    @Query("SELECT m.organizationId FROM OrganizationMemberJpaEntity m WHERE m.userId = :userId AND m.status = 'ACTIVE' ORDER BY m.joinedAt ASC LIMIT 1")
    UUID findOrganizationIdByUserId(@Param("userId") UUID userId);

    @Query("SELECT m.role FROM OrganizationMemberJpaEntity m WHERE m.userId = :userId AND m.status = 'ACTIVE' ORDER BY m.joinedAt ASC LIMIT 1")
    String findOrgRoleByUserId(@Param("userId") UUID userId);

    @Query("SELECT m.role FROM OrganizationMemberJpaEntity m WHERE m.organizationId = :organizationId AND m.userId = :userId AND m.status = 'ACTIVE'")
    Optional<String> findRoleByOrgAndUser(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

        @Modifying
        @Query("UPDATE OrganizationMemberJpaEntity m SET m.role = :role WHERE m.organizationId = :organizationId AND m.userId = :userId")
        void updateRole(@Param("organizationId") UUID organizationId,
                                        @Param("userId") UUID userId,
                                        @Param("role") String role);

    /**
     * Fetches username from the users table in the same round-trip.
     */
    @Query("""
            SELECT m.organizationId AS organizationId,
                   o.name          AS organizationName,
                   m.userId         AS userId,
                   u.username       AS username,
                   m.role           AS role,
                   m.status         AS status,
                    m.joinedAt       AS joinedAt
            FROM OrganizationMemberJpaEntity m
            JOIN m.user u
            JOIN OrganizationJpaEntity o ON o.organizationId = m.organizationId
            WHERE m.organizationId = :organizationId
            """)
    Page<OrganizationMemberProjection> findByOrganizationId(
            @Param("organizationId") UUID organizationId, Pageable pageable);
}
