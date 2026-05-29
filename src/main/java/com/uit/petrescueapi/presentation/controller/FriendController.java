package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.chat.FriendRequestDto;
import com.uit.petrescueapi.application.dto.chat.FriendSummaryDto;
import com.uit.petrescueapi.application.dto.comment.CursorPageDto;
import com.uit.petrescueapi.application.port.command.FriendCommandPort;
import com.uit.petrescueapi.application.port.query.FriendQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
@Tag(name = "Friends", description = "Friend requests and contacts")
public class FriendController {

    private final FriendCommandPort commandPort;
    private final FriendQueryPort queryPort;

    @PostMapping("/requests")
    @Operation(summary = "Send a friend request")
    public ResponseEntity<ApiResponse<FriendRequestDto>> sendRequest(
            @RequestParam UUID addresseeId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(commandPort.sendRequest(userId, addresseeId)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(summary = "Accept a friend request")
    public ResponseEntity<ApiResponse<FriendRequestDto>> acceptRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(commandPort.acceptRequest(requestId, userId)));
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Reject a friend request")
    public ResponseEntity<ApiResponse<FriendRequestDto>> rejectRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(commandPort.rejectRequest(requestId, userId)));
    }

    @GetMapping
    @Operation(summary = "List friends for current user")
    public ResponseEntity<ApiResponse<CursorPageDto<FriendSummaryDto>>> listFriends(
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(queryPort.listFriendsByCursor(userId, cursor, pageSize, search)));
    }

    @GetMapping("/requests/pending")
    @Operation(summary = "List pending friend requests for current user")
    public ResponseEntity<ApiResponse<CursorPageDto<FriendRequestDto>>> listPending(
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(queryPort.listPendingRequestsByCursor(userId, cursor, pageSize, search)));
    }
}
