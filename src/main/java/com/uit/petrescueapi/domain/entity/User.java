package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.UserStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User — aggregate root for identity & access.
 * Pure domain entity: no JPA annotations, no framework coupling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;
    private String userCode;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private String gender;
    private String streetAddress;
    private String wardCode;
    private String wardName;
    private String provinceCode;
    private String provinceName;
    private String expoPushToken;

    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Domain behaviour ────────────────────────

    public void verifyEmail() {
        this.emailVerified = true;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    public void resetPassword(String hashedPassword) {
        this.passwordHash  = hashedPassword;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void ban() {
        this.status = UserStatus.BANNED;
        this.updatedAt = LocalDateTime.now();
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasRole(String roleCode) {
        return roles.stream().anyMatch(r -> r.getCode().equalsIgnoreCase(roleCode));
    }
}
