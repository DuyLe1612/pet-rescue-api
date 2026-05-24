package com.uit.petrescueapi.application.dto.media;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRegisterRequestDto {
    @NotBlank
    @Size(max = 255)
    private String publicId;
    @NotBlank
    @Size(max = 20)
    private String resourceType;
    @NotBlank
    @Size(max = 20)
    private String format;
    @Min(1)
    private Integer width;
    @Min(1)
    private Integer height;
    @Min(1)
    private Integer bytes;
    @NotBlank
    @Size(max = 255)
    private String folder;
}
