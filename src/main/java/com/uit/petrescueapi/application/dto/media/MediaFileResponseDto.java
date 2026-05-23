package com.uit.petrescueapi.application.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for uploaded media file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Uploaded media file reference")
public class MediaFileResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID mediaId;

    private UUID uploaderId;

    @Schema(example = "pets/buddy-01")
    private String publicId;

    @Schema(example = "https://storage.example.com/pets/buddy-01.jpg")
    private String url;

    @Schema(example = "IMAGE", allowableValues = {"IMAGE", "VIDEO"})
    private String type;

    private LocalDateTime createdAt;
}
