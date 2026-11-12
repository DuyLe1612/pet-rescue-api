package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.post.*;
import com.uit.petrescueapi.application.port.command.PostCommandPort;
import com.uit.petrescueapi.application.port.query.PostQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.PostWebMapper;
import com.uit.petrescueapi.presentation.support.PageableRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.uit.petrescueapi.domain.valueobject.PostCategory;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Posts", description = "Community post management")
public class PostController {

    private final PostCommandPort commandPort;
    private final PostQueryPort queryPort;
    private final PostWebMapper mapper;

    @PostMapping
    @Operation(
            summary = "Create a new post",
            description = "Creates a community post for the authenticated user."
    )
    public ResponseEntity<ApiResponse<PostResponseDto>> create(
            @Valid @RequestBody CreatePostRequestDto cmd,
            Authentication authentication) {
        UUID authorId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toDto(commandPort.create(cmd, authorId))));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a post",
            description = "Updates mutable post fields such as content and media."
    )
    public ResponseEntity<ApiResponse<PostResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequestDto cmd,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.update(id, cmd, requesterId))));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a post",
            description = "Soft-deletes a post by id."
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication authentication) {
        commandPort.delete(id, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get post by ID",
            description = "Returns post detail including author, media, and tags."
    )
    public ResponseEntity<ApiResponse<PostResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List posts",
            description = "Returns paginated post summaries. Optional filters on the facets the author confirmed "
                    + "(category, species, district, province, needsRescue) and on a tag code."
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponseDto>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Boolean needsRescue,
            @RequestParam(required = false) UUID rescueCaseId,
            @RequestParam(required = false) Boolean hasOpenCase,
            @RequestParam(required = false) String tag) {
        PostFeedFilter filter = new PostFeedFilter(name(category), species, district, province, needsRescue, tag, rescueCaseId, hasOpenCase);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findAll(search, filter, PageableRequestFactory.of(page, pageSize, sortBy, sortOrder)))));
    }

    @GetMapping("/feed")
    @Operation(
            summary = "Post feed by cursor",
            description = "Public. Returns post feed using cursor pagination by (createdAt, postId): pass nextCursor as cursor and "
                    + "nextCursorId as cursorId. Optional filters on confirmed post facets (category, species, district, province, "
                    + "needsRescue) and tag code. category is one of RESCUE, ADOPTION, LOST, FOUND, GENERAL. "
                    + "rescueCaseId returns the posts about one case; hasOpenCase=true the posts whose case still needs help. "
                    + "A linked post carries rescueCase with the case code, status, priority, place and first image."
    )
    public ResponseEntity<ApiResponse<PostCursorResponseDto>> getFeed(
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Boolean needsRescue,
            @RequestParam(required = false) UUID rescueCaseId,
            @RequestParam(required = false) Boolean hasOpenCase,
            @RequestParam(required = false) String tag,
            Authentication authentication) {
        // Public endpoint: a guest arrives as an anonymous token, not as null.
        UUID viewerId = authentication == null
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken
                ? null : UUID.fromString(authentication.getName());
        PostFeedFilter filter = new PostFeedFilter(name(category), species, district, province, needsRescue, tag, rescueCaseId, hasOpenCase);
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findFeedByCursor(cursor, cursorId, pageSize, viewerId, filter)));
    }

    private static String name(PostCategory category) {
        return category == null ? null : category.name();
    }
}
