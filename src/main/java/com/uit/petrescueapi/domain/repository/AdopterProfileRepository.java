package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.AdopterProfile;

import java.util.Optional;
import java.util.UUID;

public interface AdopterProfileRepository {

    Optional<AdopterProfile> findByUserId(UUID userId);

    /** Creates the profile or replaces it, raising its version. */
    AdopterProfile save(AdopterProfile profile);
}
