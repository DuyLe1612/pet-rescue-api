package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.support.DisplayName;

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

        /** The full name, or a neutral label; never the username, which people sign in with. */
        public String displayName() {
            return DisplayName.of(fullName);
        }
    }
}
