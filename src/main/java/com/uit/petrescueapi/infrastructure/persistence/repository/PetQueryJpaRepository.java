package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetSummaryProjection;
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
 * Dedicated read-only JPA repository for Pet queries (CQRS query side).
 */
@Repository
public interface PetQueryJpaRepository extends JpaRepository<PetJpaEntity, UUID> {

    // ── Summary (list views) with filters ────────────────────

    @Query(value = """
        SELECT p.pet_id AS id,
               p.pet_code AS petCode,
               p.name AS name,
               p.species AS species,
               p.breed AS breed,
               p.age AS age,
               p.is_vaccinated AS vaccinated,
               p.gender AS gender,
               p.status AS status,
               p.health_status AS healthStatus,
               (SELECT mf.public_id FROM pet_media pm JOIN media_files mf ON pm.media_file_id = mf.media_id
                WHERE pm.pet_id = p.pet_id ORDER BY pm.is_primary DESC, pm.created_at ASC LIMIT 1) AS imagePublicId,
               pco.owner_type AS ownerType,
               pco.owner_id AS ownerId,
               COALESCE(NULLIF(u.full_name, ''), u.username, own_org.name) AS ownerName,
               u.avatar_url AS ownerAvatarUrl,
               COALESCE(u.phone, own_org.phone) AS ownerPhone,
               pco.caretaker_user_id AS caretakerUserId,
               COALESCE(NULLIF(caretaker.full_name, ''), caretaker.username) AS caretakerName,
               caretaker.avatar_url AS caretakerAvatarUrl,
               caretaker.phone AS caretakerPhone,
               o.organization_id AS organizationId,
               o.name AS organizationName,
               o.province_name AS provinceName,
               CAST(NULLIF(o.province_code, '') AS INTEGER) AS provinceCode,
               o.ward_name AS wardName,
               CAST(NULLIF(o.ward_code, '') AS INTEGER) AS wardCode
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        LEFT JOIN users u ON pco.owner_type = 'USER' AND pco.owner_id = u.user_id
        LEFT JOIN organizations own_org ON pco.owner_type = 'ORGANIZATION' AND pco.owner_id = own_org.organization_id
        LEFT JOIN users caretaker ON pco.owner_type = 'ORGANIZATION' AND pco.caretaker_user_id = caretaker.user_id
        LEFT JOIN organizations o ON p.shelter_id = o.organization_id
        WHERE p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:ownerUserId IS NULL OR (pco.owner_type = 'USER' AND pco.owner_id = :ownerUserId))
          AND (:ownerOrganizationId IS NULL OR (pco.owner_type = 'ORGANIZATION' AND pco.owner_id = :ownerOrganizationId))
    """, countQuery = """
        SELECT COUNT(p.pet_id)
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        WHERE p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:ownerUserId IS NULL OR (pco.owner_type = 'USER' AND pco.owner_id = :ownerUserId))
          AND (:ownerOrganizationId IS NULL OR (pco.owner_type = 'ORGANIZATION' AND pco.owner_id = :ownerOrganizationId))
    """, nativeQuery = true)
    Page<PetSummaryProjection> findAllWithFilters(
            @Param("species") String species,
            @Param("breed") String breed,
            @Param("gender") String gender,
            @Param("name") String name,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("ownerOrganizationId") UUID ownerOrganizationId,
            Pageable pageable);

    @Query(value = """
        SELECT p.pet_id AS id,
               p.pet_code AS petCode,
               p.name AS name,
               p.species AS species,
               p.breed AS breed,
               p.age AS age,
               p.is_vaccinated AS vaccinated,
               p.gender AS gender,
               p.status AS status,
               p.health_status AS healthStatus,
               (SELECT mf.public_id FROM pet_media pm JOIN media_files mf ON pm.media_file_id = mf.media_id
                WHERE pm.pet_id = p.pet_id ORDER BY pm.is_primary DESC, pm.created_at ASC LIMIT 1) AS imagePublicId,
               pco.owner_type AS ownerType,
               pco.owner_id AS ownerId,
               COALESCE(NULLIF(u.full_name, ''), u.username, own_org.name) AS ownerName,
               u.avatar_url AS ownerAvatarUrl,
               COALESCE(u.phone, own_org.phone) AS ownerPhone,
               pco.caretaker_user_id AS caretakerUserId,
               COALESCE(NULLIF(caretaker.full_name, ''), caretaker.username) AS caretakerName,
               caretaker.avatar_url AS caretakerAvatarUrl,
               caretaker.phone AS caretakerPhone,
               o.organization_id AS organizationId,
               o.name AS organizationName,
               o.province_name AS provinceName,
               CAST(o.province_code AS INTEGER) AS provinceCode,
               o.ward_name AS wardName,
               CAST(o.ward_code AS INTEGER) AS wardCode
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        LEFT JOIN users u ON pco.owner_type = 'USER' AND pco.owner_id = u.user_id
        LEFT JOIN organizations own_org ON pco.owner_type = 'ORGANIZATION' AND pco.owner_id = own_org.organization_id
        LEFT JOIN users caretaker ON pco.owner_type = 'ORGANIZATION' AND pco.caretaker_user_id = caretaker.user_id
        LEFT JOIN organizations o ON p.shelter_id = o.organization_id
        WHERE p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND p.status IN (:statuses)
          AND (:ownerUserId IS NULL OR (pco.owner_type = 'USER' AND pco.owner_id = :ownerUserId))
          AND (:ownerOrganizationId IS NULL OR (pco.owner_type = 'ORGANIZATION' AND pco.owner_id = :ownerOrganizationId))
    """, countQuery = """
        SELECT COUNT(p.pet_id)
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        WHERE p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND p.status IN (:statuses)
          AND (:ownerUserId IS NULL OR (pco.owner_type = 'USER' AND pco.owner_id = :ownerUserId))
          AND (:ownerOrganizationId IS NULL OR (pco.owner_type = 'ORGANIZATION' AND pco.owner_id = :ownerOrganizationId))
    """, nativeQuery = true)
    Page<PetSummaryProjection> findAllWithFiltersAndStatuses(
            @Param("species") String species,
            @Param("breed") String breed,
            @Param("gender") String gender,
            @Param("name") String name,
            @Param("statuses") List<String> statuses,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("ownerOrganizationId") UUID ownerOrganizationId,
            Pageable pageable);

    @Query(value = """
        SELECT p.pet_id AS id,
               p.pet_code AS petCode,
               p.name AS name,
               p.species AS species,
               p.breed AS breed,
               p.age AS age,
               p.is_vaccinated AS vaccinated,
               p.gender AS gender,
               p.status AS status,
               p.health_status AS healthStatus,
               (SELECT mf.public_id FROM pet_media pm JOIN media_files mf ON pm.media_file_id = mf.media_id
                WHERE pm.pet_id = p.pet_id ORDER BY pm.is_primary DESC, pm.created_at ASC LIMIT 1) AS imagePublicId,
               pco.owner_type AS ownerType,
               pco.owner_id AS ownerId,
               COALESCE(NULLIF(u.full_name, ''), u.username, own_org.name) AS ownerName,
               u.avatar_url AS ownerAvatarUrl,
               COALESCE(u.phone, own_org.phone) AS ownerPhone,
               pco.caretaker_user_id AS caretakerUserId,
               COALESCE(NULLIF(caretaker.full_name, ''), caretaker.username) AS caretakerName,
               caretaker.avatar_url AS caretakerAvatarUrl,
               caretaker.phone AS caretakerPhone,
               o.organization_id AS organizationId,
               o.name AS organizationName,
               o.province_name AS provinceName,
               CAST(o.province_code AS INTEGER) AS provinceCode,
               o.ward_name AS wardName,
               CAST(o.ward_code AS INTEGER) AS wardCode
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        LEFT JOIN users u ON pco.owner_type = 'USER' AND pco.owner_id = u.user_id
        LEFT JOIN organizations own_org ON pco.owner_type = 'ORGANIZATION' AND pco.owner_id = own_org.organization_id
        LEFT JOIN users caretaker ON pco.owner_type = 'ORGANIZATION' AND pco.caretaker_user_id = caretaker.user_id
        LEFT JOIN organizations o ON p.shelter_id = o.organization_id
        WHERE p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
          AND p.status = :status
          AND (:ownerOrganizationId IS NULL OR (pco.owner_type = 'ORGANIZATION' AND pco.owner_id = :ownerOrganizationId))
    """, countQuery = """
        SELECT COUNT(p.pet_id)
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        WHERE p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
          AND p.status = :status
          AND (:ownerOrganizationId IS NULL OR (pco.owner_type = 'ORGANIZATION' AND pco.owner_id = :ownerOrganizationId))
    """, nativeQuery = true)
    Page<PetSummaryProjection> findByStatusWithFilters(
            @Param("species") String species,
            @Param("breed") String breed,
            @Param("gender") String gender,
            @Param("status") String status,
            @Param("ownerOrganizationId") UUID ownerOrganizationId,
            Pageable pageable);

    // ── Detail (single pet) ─────────────────────

    @Query(value = """
        SELECT p.pet_id AS id,
               p.pet_code AS petCode,
               p.name AS name,
               p.species AS species,
               p.breed AS breed,
               p.age AS age,
               p.gender AS gender,
               p.color AS color,
               p.weight AS weight,
               p.description AS description,
               p.status AS status,
               p.health_status AS healthStatus,
               p.is_vaccinated AS vaccinated,
               p.is_neutered AS neutered,
               p.rescue_date AS rescueDate,
                p.rescue_location AS rescueLocation,
                p.shelter_id AS shelterId,
                 (p.created_at AT TIME ZONE 'UTC') AS createdAt,
                 (p.updated_at AT TIME ZONE 'UTC') AS updatedAt,
               pco.owner_type AS ownerType,
               pco.owner_id AS ownerId,
               COALESCE(NULLIF(u.full_name, ''), u.username, own_org.name) AS ownerName,
               u.avatar_url AS ownerAvatarUrl,
               COALESCE(u.phone, own_org.phone) AS ownerPhone,
               pco.caretaker_user_id AS caretakerUserId,
               COALESCE(NULLIF(caretaker.full_name, ''), caretaker.username) AS caretakerName,
               caretaker.avatar_url AS caretakerAvatarUrl,
               caretaker.phone AS caretakerPhone,
               o.organization_id AS organizationId,
               o.name AS organizationName,
               o.province_name AS provinceName,
               CAST(o.province_code AS INTEGER) AS provinceCode,
               o.ward_name AS wardName,
               CAST(o.ward_code AS INTEGER) AS wardCode
        FROM pets p
        LEFT JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        LEFT JOIN users u ON pco.owner_type = 'USER' AND pco.owner_id = u.user_id
        LEFT JOIN organizations own_org ON pco.owner_type = 'ORGANIZATION' AND pco.owner_id = own_org.organization_id
        LEFT JOIN users caretaker ON pco.owner_type = 'ORGANIZATION' AND pco.caretaker_user_id = caretaker.user_id
        LEFT JOIN organizations o ON p.shelter_id = o.organization_id
        WHERE p.is_deleted = false AND p.pet_id = :id
    """, nativeQuery = true)
    Optional<PetDetailProjection> findDetailById(@Param("id") UUID id);

    // ── By Organization ────

    @Query(value = """
        SELECT p.pet_id AS id,
               p.pet_code AS petCode,
               p.name AS name,
               p.species AS species,
               p.breed AS breed,
               p.age AS age,
               p.is_vaccinated AS vaccinated,
               p.gender AS gender,
               p.status AS status,
               p.health_status AS healthStatus,
               (SELECT mf.public_id FROM pet_media pm JOIN media_files mf ON pm.media_file_id = mf.media_id
                WHERE pm.pet_id = p.pet_id ORDER BY pm.is_primary DESC, pm.created_at ASC LIMIT 1) AS imagePublicId,
               pco.owner_type AS ownerType,
               pco.owner_id AS ownerId,
               COALESCE(NULLIF(u.full_name, ''), u.username, own_org.name) AS ownerName,
               u.avatar_url AS ownerAvatarUrl,
               COALESCE(u.phone, own_org.phone) AS ownerPhone,
               pco.caretaker_user_id AS caretakerUserId,
               COALESCE(NULLIF(caretaker.full_name, ''), caretaker.username) AS caretakerName,
               caretaker.avatar_url AS caretakerAvatarUrl,
               caretaker.phone AS caretakerPhone,
               o.organization_id AS organizationId,
               o.name AS organizationName,
               o.province_name AS provinceName,
               CAST(o.province_code AS INTEGER) AS provinceCode,
               o.ward_name AS wardName,
               CAST(o.ward_code AS INTEGER) AS wardCode
        FROM pets p
        JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        LEFT JOIN users u ON pco.owner_type = 'USER' AND pco.owner_id = u.user_id
        LEFT JOIN organizations own_org ON pco.owner_type = 'ORGANIZATION' AND pco.owner_id = own_org.organization_id
        LEFT JOIN users caretaker ON pco.owner_type = 'ORGANIZATION' AND pco.caretaker_user_id = caretaker.user_id
        LEFT JOIN organizations o ON p.shelter_id = o.organization_id
        WHERE pco.owner_id = :organizationId
          AND pco.owner_type = 'ORGANIZATION'
          AND p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
    """, countQuery = """
        SELECT COUNT(p.pet_id)
        FROM pets p
        JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        WHERE pco.owner_id = :organizationId
          AND pco.owner_type = 'ORGANIZATION'
          AND p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
    """, nativeQuery = true)
    Page<PetSummaryProjection> findSummariesByOrganizationId(
            @Param("organizationId") UUID organizationId,
            @Param("species") String species,
            @Param("breed") String breed,
            @Param("gender") String gender,
            Pageable pageable);

    @Query(value = """
        SELECT p.pet_id AS id,
               p.pet_code AS petCode,
               p.name AS name,
               p.species AS species,
               p.breed AS breed,
               p.age AS age,
               p.is_vaccinated AS vaccinated,
               p.gender AS gender,
               p.status AS status,
               p.health_status AS healthStatus,
               (SELECT mf.public_id FROM pet_media pm JOIN media_files mf ON pm.media_file_id = mf.media_id
                WHERE pm.pet_id = p.pet_id ORDER BY pm.is_primary DESC, pm.created_at ASC LIMIT 1) AS imagePublicId,
               pco.owner_type AS ownerType,
               pco.owner_id AS ownerId,
               COALESCE(NULLIF(u.full_name, ''), u.username, own_org.name) AS ownerName,
               u.avatar_url AS ownerAvatarUrl,
               COALESCE(u.phone, own_org.phone) AS ownerPhone,
               pco.caretaker_user_id AS caretakerUserId,
               COALESCE(NULLIF(caretaker.full_name, ''), caretaker.username) AS caretakerName,
               caretaker.avatar_url AS caretakerAvatarUrl,
               caretaker.phone AS caretakerPhone,
               o.organization_id AS organizationId,
               o.name AS organizationName,
               o.province_name AS provinceName,
               CAST(o.province_code AS INTEGER) AS provinceCode,
               o.ward_name AS wardName,
               CAST(o.ward_code AS INTEGER) AS wardCode
        FROM pets p
        JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        LEFT JOIN users u ON pco.owner_type = 'USER' AND pco.owner_id = u.user_id
        LEFT JOIN organizations own_org ON pco.owner_type = 'ORGANIZATION' AND pco.owner_id = own_org.organization_id
        LEFT JOIN users caretaker ON pco.owner_type = 'ORGANIZATION' AND pco.caretaker_user_id = caretaker.user_id
        LEFT JOIN organizations o ON p.shelter_id = o.organization_id
        WHERE pco.owner_id = :userId
          AND pco.owner_type = 'USER'
          AND p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
    """, countQuery = """
        SELECT COUNT(p.pet_id)
        FROM pets p
        JOIN pets_current_owner pco ON pco.pet_id = p.pet_id
        WHERE pco.owner_id = :userId
          AND pco.owner_type = 'USER'
          AND p.is_deleted = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR p.breed = :breed)
          AND (:gender IS NULL OR p.gender = :gender)
    """, nativeQuery = true)
    Page<PetSummaryProjection> findSummariesByUserId(
            @Param("userId") UUID userId,
            @Param("species") String species,
            @Param("breed") String breed,
            @Param("gender") String gender,
            Pageable pageable);

    // ── Image public_ids ──

    @Query(value = """
            SELECT mf.public_id
            FROM pet_media pm
            JOIN media_files mf ON pm.media_file_id = mf.media_id
            JOIN pets p ON pm.pet_id = p.pet_id
            WHERE p.pet_id = :id AND p.is_deleted = false
          ORDER BY pm.is_primary DESC, pm.created_at ASC
            """, nativeQuery = true)
    List<String> findImagePublicIdsById(@Param("id") UUID id);

        @Query(value = """
          SELECT mf.public_id
          FROM pet_media pm
          JOIN media_files mf ON pm.media_file_id = mf.media_id
          JOIN pets p ON pm.pet_id = p.pet_id
          WHERE p.pet_id = :id AND p.is_deleted = false AND pm.is_primary = TRUE
          LIMIT 1
          """, nativeQuery = true)
        Optional<String> findPrimaryImagePublicIdById(@Param("id") UUID id);
}
