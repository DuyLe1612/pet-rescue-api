package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueCompletionMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RescueCompletionMediaRepository {
    RescueCompletionMedia Save(RescueCompletionMedia rescueCompletionMedia);
    Optional<RescueCompletionMedia> findById(UUID id);
    Page<RescueCompletionMedia> findByCaseId(UUID caseId, Pageable pageable);
}
