package com.uit.petrescueapi.application.dto.post;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequestDto {

    @Size(max = 2000)
    private String content;
    private UUID rescueCaseId;

    @Size(max = 10)
    private List<UUID> mediaIds;

    @Size(max = 20)
    private List<String> tagCodes;
}
