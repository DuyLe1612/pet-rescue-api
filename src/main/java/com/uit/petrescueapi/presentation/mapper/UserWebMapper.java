package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserWebMapper {
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToCodes")
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "organizationName", ignore = true)
    @Mapping(target = "organizationRole", ignore = true)
    UserResponseDto toDto(User user);

    @Named("rolesToCodes")
    default List<String> rolesToCodes(Set<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream().map(Role::getCode).toList();
    }
}
