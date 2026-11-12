package com.uit.petrescueapi.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Looks up the PostgreSQL rows a post references (tags, rescue case; media go through {@link PostMediaPort}). MongoDB has no
 * foreign keys, so the post adapter validates references and snapshots what the feed renders.
 */
public interface PostReferencePort {

    /** Subset of {@code codes} that exist as non-deleted tags. */
    Set<String> findExistingTagCodes(Collection<String> codes);

    Optional<String> findRescueCaseStatus(UUID caseId);

    /** What a feed card shows for each case, read fresh from PostgreSQL. Deleted cases are absent. */
    java.util.Map<UUID, RescueCaseCard> findRescueCaseCards(Collection<UUID> caseIds);

    record RescueCaseCard(UUID caseId, String caseCode, String status, String priority, String species,
                          String locationText, String provinceName, String firstImagePublicId) {
    }
}
