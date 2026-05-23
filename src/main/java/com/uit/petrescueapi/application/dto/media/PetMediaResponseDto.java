package com.uit.petrescueapi.application.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetMediaResponseDto {
    private UUID mediaId;
    private UUID petId;
    private UUID mediaFileId;
    private String url;
    private String type;
    private boolean primaryMedia;
    private LocalDateTime createdAt;
}
