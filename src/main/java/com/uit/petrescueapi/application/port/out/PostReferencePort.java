package com.uit.petrescueapi.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Looks up the PostgreSQL rows a post references (media, tags, rescue case). MongoDB has no
 * foreign keys, so the post adapter validates references and snapshots what the feed renders.
 */
public interface PostReferencePort {

    /** mediaId → Cloudinary public id, only for media that exist and are not deleted. */
    Map<UUID, String> findMediaPublicIds(Collection<UUID> mediaIds);

    /** Subset of {@code codes} that exist as non-deleted tags. */
    Set<String> findExistingTagCodes(Collection<String> codes);

    Optional<String> findRescueCaseStatus(UUID caseId);
}
