package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.analysis.ConfirmPostAnalysisRequestDto;
import com.uit.petrescueapi.application.dto.analysis.CreateRescueCaseFromPostRequestDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisResponseDto;
import com.uit.petrescueapi.domain.entity.RescueCase;

import java.util.List;
import java.util.UUID;

/** DA2 workflow: Post → AI Post Processing → (author confirms) → Rescue Case. */
public interface PostAnalysisCommandPort {

    /** Runs the model for one post (worker entry point); no-op for confirmed/rejected posts. */
    void analyze(UUID postId);

    PostAnalysisResponseDto getAnalysis(UUID postId);

    PostAnalysisResponseDto confirm(UUID postId, ConfirmPostAnalysisRequestDto request, UUID userId);

    PostAnalysisResponseDto reject(UUID postId, UUID userId);

    /** Puts the post back in the queue; the worker picks it up. */
    PostAnalysisResponseDto retry(UUID postId, UUID userId);

    RescueCase createRescueCase(UUID postId, CreateRescueCaseFromPostRequestDto request, UUID userId);

    List<PostAnalysisMetricsDto> metrics();
}
