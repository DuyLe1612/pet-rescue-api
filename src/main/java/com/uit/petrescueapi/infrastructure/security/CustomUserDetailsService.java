package com.uit.petrescueapi.infrastructure.security;

import com.uit.petrescueapi.infrastructure.persistence.entity.UserJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link UserDetails} from the database for Spring Security.
 * The {@code username} field in the security context stores the user's UUID.
 *
 * <p><strong>@Transactional is critical</strong> — ensures Hibernate session
 * remains open while loading user and eagerly fetching roles to avoid
 * LazyInitializationException in JWT authentication filter.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)  // ← CRITICAL: Keeps Hibernate session open
    public UserDetails loadUserByUsername(String userIdOrEmail) throws UsernameNotFoundException {
        // This runs on every authenticated request, and a JWT subject is always a UUID.
        java.util.Optional<UserJpaEntity> opt = parseUuid(userIdOrEmail)
                .map(userJpaRepository::findByIdWithRoles)
                .orElseGet(() -> userJpaRepository.findByEmailWithRoles(userIdOrEmail));

        UserJpaEntity entity = opt.orElseThrow(() -> new UsernameNotFoundException("User not found: " + userIdOrEmail));

        var authorities = entity.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
                .toList();

        return new org.springframework.security.core.userdetails.User(
                entity.getUserId().toString(),
                entity.getPasswordHash() != null ? entity.getPasswordHash() : "",
                entity.isEmailVerified(),      // enabled only if verified
                true,                          // accountNonExpired
                true,                          // credentialsNonExpired
                !"BANNED".equals(entity.getStatus().name()) && !"LOCKED".equals(entity.getStatus().name()),
                authorities
        );
    }

    private static java.util.Optional<java.util.UUID> parseUuid(String value) {
        try {
            return java.util.Optional.of(java.util.UUID.fromString(value));
        } catch (IllegalArgumentException notAUuid) {
            return java.util.Optional.empty();
        }
    }
}
