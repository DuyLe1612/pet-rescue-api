package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Tag;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the Tag entity.
 */
public interface TagRepository {

    Tag save(Tag tag);

    Optional<Tag> findById(UUID tagId);

    Optional<Tag> findByCode(String code);
}
