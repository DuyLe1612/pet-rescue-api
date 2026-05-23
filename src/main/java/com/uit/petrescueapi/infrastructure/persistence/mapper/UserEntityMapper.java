package com.uit.petrescueapi.infrastructure.persistence.mapper;

import com.uit.petrescueapi.domain.entity.Role;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.infrastructure.persistence.entity.RoleJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = RoleEntityMapper.class)
public interface UserEntityMapper {

    @Mapping(source = "userId", target = "id")
        @Mapping(source = "expoPushToken", target = "expoPushToken")
        User toDomain(UserJpaEntity entity);

        @Mapping(source = "id", target = "userId")
        @Mapping(source = "expoPushToken", target = "expoPushToken")
        UserJpaEntity toEntity(User domain);

    default Set<Role> mapRoles(Set<RoleJpaEntity> entities) {
        if (entities == null) return Set.of();
        return entities.stream()
                .map(e -> Role.builder()
                        .id(e.getRoleId())
                        .code(e.getCode())
                        .name(e.getName())
                        .description(e.getDescription())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toSet());
    }

    default Set<RoleJpaEntity> mapRoleDomains(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(r -> RoleJpaEntity.builder()
                        .roleId(r.getId())
                        .code(r.getCode())
                        .name(r.getName())
                        .description(r.getDescription())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toSet());
    }
}
