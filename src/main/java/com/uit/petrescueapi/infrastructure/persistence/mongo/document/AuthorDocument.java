package com.uit.petrescueapi.infrastructure.persistence.mongo.document;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Author snapshot embedded in posts and comments (migrate.md §6). Kept in sync by the
 * user-profile listener. Field is {@code userId}, not {@code id}, to avoid Spring Data's {@code _id} mapping.
 * The username is deliberately not copied: people sign in with it, so it never leaves the users table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDocument {
    private UUID userId;
    private String fullName;
    private String avatarUrl;

    public static AuthorDocument of(UUID userId, UserSnapshot snapshot) {
        return snapshot == null
                ? AuthorDocument.builder().userId(userId).build()
                : AuthorDocument.builder()
                        .userId(userId)
                        .fullName(snapshot.fullName())
                        .avatarUrl(snapshot.avatarUrl())
                        .build();
    }
}
