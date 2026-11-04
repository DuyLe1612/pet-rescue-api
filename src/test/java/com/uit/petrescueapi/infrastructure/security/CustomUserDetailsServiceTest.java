package com.uit.petrescueapi.infrastructure.security;

import com.uit.petrescueapi.domain.valueobject.UserStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.UserJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    final UserJpaRepository users = mock(UserJpaRepository.class);
    final CustomUserDetailsService service = new CustomUserDetailsService(users);

    @Test
    void aJwtSubjectIsLookedUpByIdWithoutTryingItAsAnEmail() {
        UUID id = UUID.randomUUID();
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(user(id)));

        assertThat(service.loadUserByUsername(id.toString()).getUsername()).isEqualTo(id.toString());

        verify(users, never()).findByEmailWithRoles(any());
    }

    @Test
    void anEmailIsStillAccepted() {
        UUID id = UUID.randomUUID();
        when(users.findByEmailWithRoles("a@b.test")).thenReturn(Optional.of(user(id)));

        assertThat(service.loadUserByUsername("a@b.test").getUsername()).isEqualTo(id.toString());
    }

    @Test
    void anUnknownUserIsRejected() {
        when(users.findByEmailWithRoles("nobody@b.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@b.test")).isInstanceOf(UsernameNotFoundException.class);
    }

    private static UserJpaEntity user(UUID id) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setUserId(id);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setEmailVerified(true);
        entity.setRoles(new HashSet<>());
        return entity;
    }
}
