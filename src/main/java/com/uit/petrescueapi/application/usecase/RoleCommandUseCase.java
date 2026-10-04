package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.role.CreateRoleRequestDto;
import com.uit.petrescueapi.application.port.command.RoleCommandPort;
import com.uit.petrescueapi.domain.entity.Role;
import com.uit.petrescueapi.domain.service.RoleDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Command (write) use-case for Role operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link RoleDomainService}.
 *
 * <p>Role → permission assignment is intentionally not exposed yet — the
 * underlying role-permission join is not mapped, so the endpoint would
 * silently no-op. This will be re-introduced in a follow-up task once the
 * role_permissions join table is mapped onto {@code RoleJpaEntity}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleCommandUseCase implements RoleCommandPort {

    private final RoleDomainService domainService;

    @Override
    public Role create(CreateRoleRequestDto cmd) {
        log.debug("Command: create role with code '{}'", cmd.getCode());
        Role role = Role.builder()
                .code(cmd.getCode())
                .name(cmd.getName())
                .description(cmd.getDescription())
                .build();
        return domainService.create(role);
    }

    @Override
    public void delete(Integer roleId) {
        log.debug("Command: delete role {}", roleId);
        domainService.delete(roleId);
    }
}
