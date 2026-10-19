package com.uit.petrescueapi.application.dto.analysis;

import com.uit.petrescueapi.domain.valueobject.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The author's accepted (possibly edited) version of the AI suggestion. */
@Schema(description = "Confirm or correct the AI analysis of a post")
public record ConfirmPostAnalysisRequestDto(
        @NotNull PostCategory category,
        @Schema(example = "Dog", description = "Pet species name from the taxonomy (English canonical name)")
        String species,
        @Size(max = 100) @Schema(example = "Quận 7") String district,
        @Size(max = 100) @Schema(example = "TP. Hồ Chí Minh") String province,
        boolean needsRescue,
        @Size(max = 10) List<String> tags) {
}
