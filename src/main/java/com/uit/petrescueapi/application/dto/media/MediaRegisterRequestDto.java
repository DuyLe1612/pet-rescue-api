package com.uit.petrescueapi.application.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRegisterRequestDto {
    private String publicId;
    private String resourceType;
    private String format;
    private Integer width;
    private Integer height;
    private Integer bytes;
    private String folder;
}
