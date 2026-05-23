package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.chat.ChatMessageDto;
import com.uit.petrescueapi.application.dto.chat.ConversationCursorResponseDto;
import com.uit.petrescueapi.application.dto.chat.CreateConversationRequestDto;
import com.uit.petrescueapi.application.dto.chat.CreateMessageRequestDto;
import com.uit.petrescueapi.application.dto.chat.MarkReadRequestDto;
import com.uit.petrescueapi.application.dto.chat.ConversationSummaryDto;
import com.uit.petrescueapi.application.port.command.ChatCommandPort;
import com.uit.petrescueapi.application.port.query.ChatQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import com.uit.petrescueapi.presentation.mapper.ChatWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
@Tag(name = "Chats", description = "Chat conversations and messages")
public class ChatController {

    private final ChatCommandPort commandPort;
    private final ChatQueryPort queryPort;
    private final ChatWebMapper mapper;

    @PostMapping("/conversations")
    @Operation(summary = "Create or get a direct conversation")
    public ResponseEntity<ApiResponse<ConversationSummaryDto>> createConversation(
            @RequestBody CreateConversationRequestDto request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toConversationDto(commandPort.createConversation(request, userId))));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversations by cursor for current user")
    public ResponseEntity<ApiResponse<ConversationCursorResponseDto>> listConversations(
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(queryPort.listConversationsByCursor(userId, cursor, size)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get messages in a conversation (paged)")
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageDto>>> listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(queryPort.listMessages(conversationId, userId, PageRequest.of(page, size)))));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message to a conversation")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody CreateMessageRequestDto request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toMessageDto(commandPort.sendMessage(conversationId, request, userId))));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Mark conversation as read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID conversationId,
            @RequestBody(required = false) MarkReadRequestDto request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        commandPort.markRead(conversationId, request == null ? new MarkReadRequestDto() : request, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
