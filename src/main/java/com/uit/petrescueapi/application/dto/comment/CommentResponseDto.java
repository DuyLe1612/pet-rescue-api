package com.uit.petrescueapi.application.dto.comment;

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
public class CommentResponseDto {
    private UUID commentId;
    private UUID postId;
    private UUID parentCommentId;
    private UUID authorId;
    /** Full name, or "Thành viên PawHome" when the author gave none. Never the username. */
    private String authorName;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
