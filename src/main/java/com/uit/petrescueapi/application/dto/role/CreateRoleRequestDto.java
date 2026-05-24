package com.uit.petrescueapi.application.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating a role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequestDto {

    @NotBlank
    @Size(max = 50)
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 500)
    private String description;
}
