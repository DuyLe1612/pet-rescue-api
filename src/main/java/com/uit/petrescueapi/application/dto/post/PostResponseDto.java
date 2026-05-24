package com.uit.petrescueapi.application.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for community post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Community post")
public class PostResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID postId;

    private UUID authorId;

    @Schema(example = "johndoe")
    private String authorUsername;

    private UUID rescueCaseId;

    @Schema(example = "Found this little guy near the park, he needs help!")
    private String content;

    private List<String> imageUrls;
    private List<String> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
