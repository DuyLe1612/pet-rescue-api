package com.uit.petrescueapi.application.dto.organization;

import com.uit.petrescueapi.domain.valueobject.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationWithRoleResponseDto {
    private OrganizationResponseDto organization;
    private OrganizationRole role;
}
