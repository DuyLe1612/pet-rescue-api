package com.uit.petrescueapi.application.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Which model analyses posts")
public record PostAnalysisModelDto(
        @Schema(example = "claude-haiku-5-5", description = "Model the next call will use")
        String activeModel,
        @Schema(example = "claude-opus-5-5", description = "Configured default (AI_POST_ANALYSIS_MODEL)")
        String defaultModel,
        @Schema(description = "True when an admin switched away from the default at runtime")
        boolean overridden,
        List<String> allowedModels,
        @Schema(description = "False when AI_POST_ANALYSIS_ENABLED is off: no model is called at all")
        boolean aiEnabled) {
}
