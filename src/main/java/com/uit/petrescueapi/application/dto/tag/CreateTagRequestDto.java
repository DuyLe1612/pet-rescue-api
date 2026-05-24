package com.uit.petrescueapi.application.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating a new tag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTagRequestDto {

    @NotBlank
    @Size(max = 50)
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 500)
    private String description;
}
