package com.uit.petrescueapi.application.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Accuracy of the AI analysis against human-confirmed labels, per prompt version and model (DA2 Goal 4).
 * Accuracy fields are {@code correct / labelled}; null when nothing is labelled yet.
 */
@Schema(description = "Post analysis evaluation for one prompt version and model")
public record PostAnalysisMetricsDto(
        int promptVersion,
        String model,
        long runs,
        long failed,
        long withValidationErrors,
        long labelled,
        Double categoryAccuracy,
        Double speciesAccuracy,
        Double districtAccuracy,
        Double needsRescueAccuracy) {
}
