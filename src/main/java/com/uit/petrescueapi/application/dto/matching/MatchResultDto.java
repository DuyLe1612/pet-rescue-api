package com.uit.petrescueapi.application.dto.matching;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uit.petrescueapi.domain.valueobject.MatchCertainty;
import com.uit.petrescueapi.domain.valueobject.MatchLevel;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * A match as the API shows it. Adopters get the level, the certainty, the reasons and what to
 * prepare; the number is for the organization only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Compatibility between an adopter and a pet")
public record MatchResultDto(
        MatchLevel level,
        @Schema(description = "0-100. Only for organization staff; null when a warning fired.") Double score,
        MatchCertainty certainty,
        Integer weightsVersion,
        List<MatchResult.Reason> reasons,
        List<MatchResult.Warning> warnings,
        List<String> preparation,
        @Schema(description = "On an application: the applicant confirmed they read the warnings") Boolean warningsAcknowledged) {

    public static MatchResultDto forAdopter(MatchResult result) {
        return new MatchResultDto(result.level(), null, result.certainty(), result.weightsVersion(),
                result.reasons(), result.warnings(), result.preparation(), null);
    }

    public static MatchResultDto forStaff(MatchResult result) {
        return new MatchResultDto(result.level(), result.score(), result.certainty(), result.weightsVersion(),
                result.reasons(), result.warnings(), result.preparation(), null);
    }

    public MatchResultDto withoutScore() {
        return new MatchResultDto(level, null, certainty, weightsVersion, reasons, warnings, preparation, warningsAcknowledged);
    }

    public MatchResultDto acknowledged(boolean value) {
        return new MatchResultDto(level, score, certainty, weightsVersion, reasons, warnings, preparation, value);
    }
}
