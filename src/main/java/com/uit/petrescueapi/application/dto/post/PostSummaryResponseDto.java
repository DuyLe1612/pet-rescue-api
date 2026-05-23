package com.uit.petrescueapi.application.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import com.uit.petrescueapi.application.dto.media.MediaFileResponseDto;
import java.util.UUID;

/**
 * Lightweight response DTO for post list views (no media attachments).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Post summary for list views")
public class PostSummaryResponseDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID postId;

    private UUID authorId;

    @Schema(example = "johndoe")
    private String authorUsername;

    @Schema(example = "Found this little guy near the park, he needs help!")
    private String content;

    private List<String> tags;
    private Integer likeCount;
    private Integer commentCount;

    private LocalDateTime createdAt;
    private List<MediaFileResponseDto> media;
}
