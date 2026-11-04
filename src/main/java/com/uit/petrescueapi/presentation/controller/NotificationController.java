package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.notification.NotificationResponseDto;
import com.uit.petrescueapi.application.port.command.NotificationCommandPort;
import com.uit.petrescueapi.application.port.query.NotificationQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications of the current user")
public class NotificationController {

    private final NotificationCommandPort commandPort;
    private final NotificationQueryPort queryPort;

    @GetMapping
    @Operation(summary = "List my notifications, newest first")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponseDto>>> getMine(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                queryPort.findMine(userId(authentication), unreadOnly, PageRequest.of(page, pageSize)))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count my unread notifications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", queryPort.countUnread(userId(authentication)))));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID id, Authentication authentication) {
        commandPort.markRead(id, userId(authentication));
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", commandPort.markAllRead(userId(authentication)))));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
