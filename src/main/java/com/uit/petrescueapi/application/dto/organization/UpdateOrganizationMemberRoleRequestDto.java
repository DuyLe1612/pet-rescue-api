package com.uit.petrescueapi.application.dto.organization;

import com.uit.petrescueapi.domain.valueobject.OrganizationRole;
import com.uit.petrescueapi.application.support.CanonicalEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationMemberRoleRequestDto {

    @NotBlank
    @Pattern(regexp = "^(OWNER|STAFF|VET|MEMBER)$")
    @CanonicalEnum(OrganizationRole.class)
    private String role;
}
