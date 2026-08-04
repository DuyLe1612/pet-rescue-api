package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.ReclaimLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReclaimLogRepository {
    ReclaimLog save(ReclaimLog reclaimLog);
    Page<ReclaimLog> findAll(Pageable pageable);
    Page<ReclaimLog> findByOrganizationId(UUID organizationId, Pageable pageable);
}
