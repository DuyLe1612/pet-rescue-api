package com.uit.petrescueapi.application.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Switch the post analysis model")
public record SwitchPostAnalysisModelRequestDto(
        @NotBlank @Schema(example = "claude-haiku-5-5") String model) {
}
