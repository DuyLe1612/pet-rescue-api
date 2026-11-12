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
    private String authorAvatarUrl;
    private Integer likeCount;
    private Integer commentCount;

    private UUID rescueCaseId;

    @Schema(example = "Found this little guy near the park, he needs help!")
    private String content;

    private List<String> imageUrls;
    private List<String> tags;

    @Schema(description = "The rescue case this post is about. Null when the post is not linked to a case.")
    private PostRescueCaseDto rescueCase;

    @Schema(description = "Labels confirmed by the author (category, species, area, needsRescue). Null until confirmed.")
    private com.uit.petrescueapi.application.dto.analysis.PostFacets facets;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
