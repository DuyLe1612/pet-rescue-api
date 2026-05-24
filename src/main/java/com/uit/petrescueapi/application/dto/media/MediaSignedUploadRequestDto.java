package com.uit.petrescueapi.application.dto.media;

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
public class MediaSignedUploadRequestDto {
    @NotBlank
    @Size(max = 255)
    private String folder;
    @NotBlank
    @Size(max = 255)
    private String publicId;
}
