package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.organization.OrganizationAdminResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationMemberResponseDto;
import com.uit.petrescueapi.application.dto.organization.OrganizationResponseDto;
import com.uit.petrescueapi.domain.entity.Organization;
import com.uit.petrescueapi.domain.entity.OrganizationMember;
import com.uit.petrescueapi.domain.valueobject.OrganizationStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper: domain entities → presentation DTOs for Organization module.
 *
 * <ul>
 *   <li>{@link OrganizationResponseDto} — full detail / write response for regular users</li>
 *   <li>{@link OrganizationAdminResponseDto} — full detail for admin/org-owner with all fields</li>
 *   <li>{@link OrganizationMemberResponseDto} — member write response (username not available on command path)</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface OrganizationWebMapper {

    /**
     * Maps domain Organization → response DTO.
     * All fields mapped directly; status (enum) is converted to string.
     */
    @Mapping(target = "status", source = "status")
    OrganizationResponseDto toDto(Organization organization);

    /**
     * Maps domain Organization → admin response DTO (includes all fields).
     */
    @Mapping(target = "status", source = "status")
    OrganizationAdminResponseDto toAdminDto(Organization organization);

    /**
     * Convert OrganizationStatus enum to string for API response.
     */
    default String map(OrganizationStatus value) {
        if (value == null) return null;
        return value.name();
    }

    /**
     * Maps domain OrganizationMember → response DTO.
     * {@code username} is not present in the domain entity (resolved only by JOIN on query path)
     * so it is ignored here and will be null in command responses.
     */
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "organizationName", ignore = true)
    OrganizationMemberResponseDto toMemberDto(OrganizationMember member);
}
