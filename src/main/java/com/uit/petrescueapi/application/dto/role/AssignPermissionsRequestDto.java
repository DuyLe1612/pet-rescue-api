package com.uit.petrescueapi.application.dto.role;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Request DTO for assigning permissions to a role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionsRequestDto {

    @NotEmpty
    @Size(max = 100)
    private List<Integer> permissionIds;
}
