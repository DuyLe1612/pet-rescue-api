package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.comment.CommentResponseDto;
import com.uit.petrescueapi.application.dto.comment.CommentSummaryDto;
import com.uit.petrescueapi.application.dto.comment.CreateCommentRequestDto;
import com.uit.petrescueapi.application.dto.comment.CursorPageDto;
import com.uit.petrescueapi.application.port.command.CommentCommandPort;
import com.uit.petrescueapi.application.port.query.CommentQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.CommentWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment and reply operations for posts")
public class CommentController {

    private final CommentCommandPort commentCommandPort;
    private final CommentQueryPort commentQueryPort;
    private final CommentWebMapper commentWebMapper;

    @PostMapping("/posts/{postId}/comments")
    @Operation(
            summary = "Create a comment",
            description = "Creates a parent comment (when parentCommentId is null) or a reply (child comment) under an existing parent comment."
    )
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequestDto cmd,
            Authentication authentication) {
        UUID authorId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(commentWebMapper.toDto(commentCommandPort.createComment(postId, cmd, authorId)))
        );
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(
            summary = "List parent comments by post",
            description = "Returns parent comments only (parentCommentId is null) with cursor pagination support."
    )
    public ResponseEntity<ApiResponse<CursorPageDto<CommentSummaryDto>>> getCommentsByPost(
            @PathVariable UUID postId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int pageSize,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                                commentQueryPort.findParentCommentsByPostId(postId, cursor, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder))
        ));
    }

    @GetMapping("/comments/{id}")
    @Operation(
            summary = "Get comment detail",
            description = "Returns detail of a single comment by id."
    )
    public ResponseEntity<ApiResponse<CommentResponseDto>> getCommentById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(commentQueryPort.findById(id)));
    }

    @GetMapping("/comments/{id}/replies")
    @Operation(
            summary = "List replies of a comment",
            description = "Returns child replies for a parent comment id."
    )
    public ResponseEntity<ApiResponse<PageResponse<CommentSummaryDto>>> getReplies(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int pageSize,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(ApiResponse.ok(
                                PageResponse.from(commentQueryPort.findRepliesByParentCommentId(id, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))
        ));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(
            summary = "Delete a comment",
            description = "Soft-deletes a comment by id."
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID id) {
        commentCommandPort.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
