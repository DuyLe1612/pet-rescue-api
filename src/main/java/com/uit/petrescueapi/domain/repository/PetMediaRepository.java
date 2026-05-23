package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.PetMedia;

import java.util.List;
import java.util.UUID;

/**
 * Domain repository contract for the PetMedia entity.
 */
public interface PetMediaRepository {

    PetMedia save(PetMedia petMedia);

    List<PetMedia> findByPetId(UUID petId);

    java.util.Optional<PetMedia> findPrimaryByPetId(UUID petId);

    java.util.Optional<PetMedia> findByPetIdAndMediaFileId(UUID petId, UUID mediaFileId);

    void clearPrimary(UUID petId);

    void setPrimary(UUID petId, UUID mediaId);

    void deleteByPetIdAndMediaId(UUID petId, UUID mediaId);

    void delete(UUID mediaId);
}
