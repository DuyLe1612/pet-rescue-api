package com.uit.petrescueapi.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only lookup of the user fields that are denormalised into MongoDB documents
 * (author / chat peer snapshots). Never loads lazy relations, so it is safe to call
 * inside a MongoDB transaction where no JPA session is open.
 */
public interface UserSnapshotPort {

    Map<UUID, UserSnapshot> findByIds(Collection<UUID> ids);

    record UserSnapshot(UUID id, String username, String fullName, String avatarUrl) {

        /** Same rule as the old SQL: {@code COALESCE(NULLIF(full_name, ''), username)}. */
        public String displayName() {
            return fullName != null && !fullName.isBlank() ? fullName : username;
        }
    }
}
