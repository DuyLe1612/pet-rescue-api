package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.analysis.ConfirmPostAnalysisRequestDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisMetricsDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisModelDto;
import com.uit.petrescueapi.application.dto.analysis.PostAnalysisResponseDto;

import java.util.List;
import java.util.UUID;

/** Post → AI classification → author confirms. Classification never creates a rescue case. */
public interface PostAnalysisCommandPort {

    /** Runs the model for one post (worker entry point); no-op for confirmed/rejected posts. */
    void analyze(UUID postId);

    PostAnalysisResponseDto getAnalysis(UUID postId);

    PostAnalysisResponseDto confirm(UUID postId, ConfirmPostAnalysisRequestDto request, UUID userId);

    PostAnalysisResponseDto reject(UUID postId, UUID userId);

    /** Puts the post back in the queue; the worker picks it up. */
    PostAnalysisResponseDto retry(UUID postId, UUID userId);

    List<PostAnalysisMetricsDto> metrics();

    PostAnalysisModelDto modelSettings();

    /** Applies from the next model call; {@code model} must be in the allowed list. */
    PostAnalysisModelDto switchModel(String model, UUID userId);

    /** Back to the configured default ({@code app.ai.post-analysis.model}). */
    PostAnalysisModelDto resetModel();
}
