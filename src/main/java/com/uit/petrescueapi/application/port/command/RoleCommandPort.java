package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.role.CreateRoleRequestDto;
import com.uit.petrescueapi.domain.entity.Role;

public interface RoleCommandPort {
    Role create(CreateRoleRequestDto cmd);
    void delete(Integer roleId);
}
