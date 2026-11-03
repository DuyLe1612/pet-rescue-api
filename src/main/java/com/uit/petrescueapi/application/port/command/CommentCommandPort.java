package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.comment.CreateCommentRequestDto;
import com.uit.petrescueapi.domain.entity.Comment;

import java.util.UUID;

public interface CommentCommandPort {
    Comment createComment(UUID postId, CreateCommentRequestDto cmd, UUID authorId);
    void deleteComment(UUID commentId, UUID requesterId);
}
