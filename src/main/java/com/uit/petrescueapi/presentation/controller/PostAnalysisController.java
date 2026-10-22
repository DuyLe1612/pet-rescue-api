package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.analysis.ConfirmPostAnalysisRequestDto;
import com.uit.petrescueapi.application.dto.analysis.CreateRescueCaseFromPostRequestDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisResponseDto;
import com.uit.petrescueapi.application.dto.rescue.RescueCaseResponseDto;
import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.mapper.RescueCaseWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DA2 Intelligent Post Processing: the author reviews the AI suggestion, confirms or corrects it,
 * and may turn a confirmed rescue post into a Rescue Case. Only available with posts on MongoDB.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Post Analysis", description = "AI post processing (suggest → confirm → rescue case)")
public class PostAnalysisController {

    private final PostAnalysisCommandPort analysis;
    private final RescueCaseWebMapper rescueCaseMapper;

    @GetMapping("/posts/{id}/analysis")
    @Operation(summary = "Get the AI analysis of a post", description = "suggestion = AI output, facets = what the author confirmed.")
    public ResponseEntity<ApiResponse<PostAnalysisResponseDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(analysis.getAnalysis(id)));
    }

    @PatchMapping("/posts/{id}/analysis/confirm")
    @Operation(summary = "Confirm or correct the AI analysis (author only)")
    public ResponseEntity<ApiResponse<PostAnalysisResponseDto>> confirm(@PathVariable UUID id,
                                                                        @Valid @RequestBody ConfirmPostAnalysisRequestDto body,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(analysis.confirm(id, body, userId(authentication))));
    }

    @PostMapping("/posts/{id}/analysis/reject")
    @Operation(summary = "Reject the AI analysis (author only)")
    public ResponseEntity<ApiResponse<PostAnalysisResponseDto>> reject(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(analysis.reject(id, userId(authentication))));
    }

    @PostMapping("/posts/{id}/analysis/retry")
    @Operation(summary = "Queue the post for another AI analysis (author only)")
    public ResponseEntity<ApiResponse<PostAnalysisResponseDto>> retry(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(analysis.retry(id, userId(authentication))));
    }

    @PostMapping("/posts/{id}/rescue-case")
    @Operation(summary = "Create a Rescue Case from a confirmed rescue post (author only)",
            description = "Species, description, location and priority are prefilled from the confirmed analysis.")
    public ResponseEntity<ApiResponse<RescueCaseResponseDto>> createRescueCase(@PathVariable UUID id,
                                                                               @RequestBody(required = false) CreateRescueCaseFromPostRequestDto body,
                                                                               Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                rescueCaseMapper.toDto(analysis.createRescueCase(id, body, userId(authentication)))));
    }

    @GetMapping("/admin/ai/post-analysis/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AI analysis accuracy vs. author-confirmed labels, per prompt version (DA2 Goal 4)")
    public ResponseEntity<ApiResponse<List<PostAnalysisMetricsDto>>> metrics() {
        return ResponseEntity.ok(ApiResponse.ok(analysis.metrics()));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
