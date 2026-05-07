package com.uit.petrescueapi.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface RescueMediaJpaRepository {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO rescue_media (case_id, media_id) VALUES (:caseId, :mediaId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void linkMediaToCase(@Param("caseId") UUID caseId, @Param("mediaId") UUID mediaId);
}
