package com.uit.petrescueapi.application.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaSignedUploadRequestDto {
    private String folder;
    private String publicId;
}
